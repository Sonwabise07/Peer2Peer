// functions/src/index.ts

// ---- Base Imports ----
import * as functions from "firebase-functions";
import * as admin from "firebase-admin"; // Standard import

import { CallableRequest, HttpsError, onCall } from "firebase-functions/v2/https";
import { onSchedule, ScheduledEvent } from "firebase-functions/v2/scheduler";
import Stripe from "stripe";
import { Timestamp, FieldValue } from "firebase-admin/firestore"; // Import FieldValue
import { onDocumentCreated, FirestoreEvent, QueryDocumentSnapshot } from "firebase-functions/v2/firestore";

// ---- Initialize Firebase Admin SDK ONCE globally ----
try {
  functions.logger.info("Attempting Firebase Admin SDK Initialization (Global Scope Entry)...");
  if (admin.apps.length === 0) { // Prevent re-initialization
    admin.initializeApp();
    functions.logger.info("Firebase Admin SDK Initialized via admin.initializeApp() successfully.");
  } else {
    functions.logger.info("Firebase Admin SDK already initialized.");
  }
} catch (e) {
  functions.logger.error("CRITICAL: Error initializing Firebase Admin SDK GLOBALLY:", e);
}

// ---- Global DB for functions OTHER THAN getDashboardStats ----
// IMPORTANT: If deployment timeouts persist, this global initialization might still be the cause.
// The most robust solution for timeouts is to lazy load Firestore for ALL functions.
const db = admin.firestore();
functions.logger.info("Global Firestore db object defined (for functions other than getDashboardStats).");


// ---- Stripe SDK Holder Variable and Initializer ----
let stripeInstance: Stripe | undefined = undefined;

function getStripe(): Stripe {
    let stripeSecret: string | undefined = process.env.STRIPE_SECRET_KEY;
    let keySource = "process.env.STRIPE_SECRET_KEY";

    if (!stripeSecret) {
        functions.logger.warn("STRIPE_SECRET_KEY environment variable not found. Attempting fallback to functions.config()...");
        const config = functions.config(); // Get config once
        const configSecret = config.stripe?.secret_key;
        if (configSecret) {
            keySource = "functions.config().stripe.secret_key";
            stripeSecret = configSecret;
        }
    }

    if (!stripeSecret || typeof stripeSecret !== 'string' || stripeSecret.trim() === '') {
         functions.logger.error("Stripe secret key is not set or invalid after checking process.env and functions.config().stripe.secret_key.");
         throw new HttpsError("internal", "Server configuration error: Missing or invalid Stripe API key.");
    }

    if (!stripeInstance) {
        functions.logger.info(`Initializing Stripe SDK LAZILY using key from: ${keySource}`);
        try {
            stripeInstance = new Stripe(stripeSecret, {
                typescript: true,
            });
            functions.logger.info("Stripe SDK Initialized LAZILY via getStripe() successfully.");
        } catch (e: any) {
            functions.logger.error("Error initializing Stripe SDK in getStripe():", e.message || e);
            throw new HttpsError("internal", "Failed to initialize Stripe SDK.");
        }
    }
    if (!stripeInstance) {
        functions.logger.error("Stripe instance is unexpectedly undefined after initialization attempt.");
        throw new HttpsError("internal", "Failed to get Stripe instance.");
    }
    return stripeInstance;
}
// --- End Stripe Init ---


// --- Interfaces ---
interface CreatePaymentIntentData {
  amount: number;
  currency?: string;
  tutorUid: string;
  slotId: string;
}
interface ConfirmBookingData {
  paymentIntentId: string;
}
interface AdminTutorActionData {
    tutorId: string;
    reason?: string;
}
interface AdminUserActionData {
    userId: string;
}
// --- End Interfaces ---


// ===========================================
// CLOUD FUNCTIONS START HERE
// ===========================================

// ---- Function 1: Create Payment Intent ----
export const createPaymentIntent = onCall<CreatePaymentIntentData>(
    async (request: CallableRequest<CreatePaymentIntentData>) => {
        let currentStripe: Stripe;
        try {
            currentStripe = getStripe();
        } catch (error: any) {
            functions.logger.error("Failed to get Stripe instance for createPaymentIntent:", error.message);
            if (error instanceof HttpsError) throw error;
            throw new HttpsError("internal", "Failed to initialize payment service.");
        }
        if (!request.auth) {
          throw new HttpsError("unauthenticated", "User must be logged in to create a payment.");
        }
        const tuteeUid = request.auth.uid;
        const { amount, currency = "zar", tutorUid, slotId } = request.data;

        if ( typeof amount !== "number" || !Number.isInteger(amount) || amount <= 0 ) {
             functions.logger.error("Invalid amount received.", { data: request.data });
             throw new HttpsError("invalid-argument", "Amount must be a positive integer (smallest currency unit, e.g., cents).");
        }
        if ( typeof tutorUid !== "string" || tutorUid.trim().length === 0 ||
             typeof slotId !== "string" || slotId.trim().length === 0 ) {
            functions.logger.error("Invalid tutorUid or slotId received.", { data: request.data });
            throw new HttpsError("invalid-argument", "Missing or invalid tutorUid/slotId.");
        }

        try {
          functions.logger.info(`Creating PI for Tutee ${tuteeUid} -> Tutor ${tutorUid}, Slot ${slotId}, Amount: ${amount} ${currency.toUpperCase()}`);
          const paymentIntent = await currentStripe.paymentIntents.create({
              amount: amount,
              currency: currency,
              automatic_payment_methods: { enabled: true },
              metadata: { tuteeUid, tutorUid, slotId },
          });
          functions.logger.info(`PaymentIntent ${paymentIntent.id} created successfully.`);
          return {
              clientSecret: paymentIntent.client_secret,
              paymentIntentId: paymentIntent.id,
          };
        } catch (error: any) {
            functions.logger.error("Error creating Stripe PaymentIntent:", error);
            if (error instanceof HttpsError) { throw error; }
            if (error.type && error.type.startsWith('Stripe')) {
                throw new HttpsError("internal", `Stripe Error: ${error.message}`);
            }
            throw new HttpsError("internal", "Unexpected error creating payment intent.");
        }
    }
);


// ---- Function 2: Confirm Booking Function ----
export const confirmBooking = onCall<ConfirmBookingData>(
    async (request: CallableRequest<ConfirmBookingData>) => {
      let currentStripe: Stripe;
      try {
        currentStripe = getStripe();
      } catch (error: any) {
        functions.logger.error("Failed to get Stripe instance for confirmBooking:", error.message);
        if (error instanceof HttpsError) throw error;
        throw new HttpsError("internal", "Failed to initialize payment service.");
      }
      if (!request.auth) {
        throw new HttpsError("unauthenticated", "User must be logged in to confirm booking.");
      }
      const callingUid = request.auth.uid;
      const { paymentIntentId } = request.data;
      if (typeof paymentIntentId !== "string" || paymentIntentId.trim().length === 0) {
          functions.logger.error("Invalid data received for confirmBooking", { data: request.data });
          throw new HttpsError("invalid-argument", "Missing or invalid paymentIntentId.");
      }

      try {
        functions.logger.info(`Verifying PaymentIntent: ${paymentIntentId} for user ${callingUid}`);
        const paymentIntent = await currentStripe.paymentIntents.retrieve(paymentIntentId);

        if (paymentIntent.status !== "succeeded") {
          functions.logger.error(`PI ${paymentIntentId} status: ${paymentIntent.status}. Not 'succeeded'.`);
          throw new HttpsError("failed-precondition", `Payment status: ${paymentIntent.status}. Booking cannot be confirmed.`);
        }
        const { tuteeUid, tutorUid, slotId } = paymentIntent.metadata;
        if (!tuteeUid || !tutorUid || !slotId) {
          functions.logger.error(`PI ${paymentIntentId} metadata missing required fields.`, { metadata: paymentIntent.metadata });
          throw new HttpsError("internal", "Payment metadata incomplete. Cannot proceed with booking.");
        }
        if (callingUid !== tuteeUid) {
          functions.logger.error(`Auth mismatch: Caller (${callingUid}) != PI Tutee (${tuteeUid}) for PI ${paymentIntentId}`);
          throw new HttpsError("permission-denied", "User does not match the payment intent.");
        }

        const existingBookingSnap = await db.collection('bookings').where('paymentIntentId', '==', paymentIntentId).limit(1).get(); // Uses global db
        if (!existingBookingSnap.empty) {
            functions.logger.warn(`Attempt to re-confirm booking with used PI ${paymentIntentId}. Existing booking: ${existingBookingSnap.docs[0].id}`);
            throw new HttpsError("already-exists", "This payment has already confirmed a booking.");
        }

        let fetchedTuteeName = "Tutee";
        let fetchedTutorName = "Tutor";
        try {
          const [tuteeDoc, tutorDoc] = await Promise.all([
              db.collection("users").doc(tuteeUid).get(), // Uses global db
              db.collection("users").doc(tutorUid).get()  // Uses global db
          ]);
          if (tuteeDoc.exists) fetchedTuteeName = tuteeDoc.data()?.name || `${tuteeDoc.data()?.firstName || ""} ${tuteeDoc.data()?.surname || ""}`.trim() || "Tutee";
          if (tutorDoc.exists) fetchedTutorName = tutorDoc.data()?.name || `${tutorDoc.data()?.firstName || ""} ${tutorDoc.data()?.surname || ""}`.trim() || "Tutor";
        } catch (nameError) { functions.logger.error("Non-critical error fetching user names:", nameError); }

        const availabilitySlotRef = db.collection("users").doc(tutorUid).collection("availability").doc(slotId); // Uses global db
        const newBookingRef = db.collection("bookings").doc(); // Uses global db
        const bookingId = newBookingRef.id;
        functions.logger.info(`Attempting booking transaction for PI ${paymentIntentId}, Booking ${bookingId}`);
        let finalModuleCode = "N/A";

        await db.runTransaction(async (transaction) => { // Uses global db
          const slotSnapshot = await transaction.get(availabilitySlotRef);
          if (!slotSnapshot.exists) throw new HttpsError("not-found", `Tutor availability slot (${slotId}) not found.`);
          const slotData = slotSnapshot.data();
          if (!slotData) throw new HttpsError("internal", `Could not read slot data ${slotId}.`);
          const currentStatus = slotData.status;
          const startTime = slotData.startTime as Timestamp;
          const endTime = slotData.endTime as Timestamp;
          const moduleCodeFromSlot = slotData.moduleCode as string;

          if (currentStatus !== "available") throw new HttpsError("failed-precondition", `Slot ${slotId} no longer available (Status: ${currentStatus}).`);
          if (!(startTime instanceof Timestamp) || !(endTime instanceof Timestamp)) throw new HttpsError("internal", "Slot time data invalid.");
          if (startTime.toDate() < new Date(Date.now() - 5 * 60 * 1000)) throw new HttpsError("failed-precondition", "Slot time has passed.");
          if (!moduleCodeFromSlot || typeof moduleCodeFromSlot !== "string" || moduleCodeFromSlot.trim() === "") {
              throw new HttpsError("internal", "Slot is missing required module information.");
          }
          finalModuleCode = moduleCodeFromSlot;

          transaction.update(availabilitySlotRef, {
              status: "booked",
              bookedBy: tuteeUid,
              bookingId: bookingId,
              updatedAt: admin.firestore.FieldValue.serverTimestamp()
          });
          const bookingData = {
            tutorUid: tutorUid, tuteeUid: tuteeUid, availabilitySlotId: slotId, startTime: startTime, endTime: endTime,
            moduleCode: moduleCodeFromSlot, bookingStatus: "confirmed", createdAt: admin.firestore.FieldValue.serverTimestamp(),
            paymentIntentId: paymentIntentId, rateCharged: paymentIntent.amount, currency: paymentIntent.currency,
            tutorName: fetchedTutorName, tuteeName: fetchedTuteeName, meetingLink: null, isRated: false,
          };
          transaction.set(newBookingRef, bookingData);
        });

        functions.logger.info(`Booking success for PI ${paymentIntentId}. Booking ID: ${bookingId}, Module: ${finalModuleCode}`);
        return { success: true, bookingId: bookingId, message: "Booking confirmed successfully!" };
      } catch (error: any) {
        functions.logger.error(`Error confirming booking for PI ${paymentIntentId}:`, error);
        if (error instanceof HttpsError) { throw error; }
        if (error.type && error.type.startsWith('Stripe')) {
            throw new HttpsError("internal", `Stripe Error during booking confirmation: ${error.message}`);
        }
        throw new HttpsError("internal", "An unexpected error occurred while confirming the booking.");
      }
    }
);

// ---- Function 3: approveTutor ----
export const approveTutor = onCall<AdminTutorActionData>(
    async (request: CallableRequest<AdminTutorActionData>) => {
        if (request.auth?.token?.isAdmin !== true) {
            functions.logger.warn("approveTutor called by non-admin.", { uid: request.auth?.uid });
            throw new HttpsError("permission-denied", "Admin privileges required.");
        }
        const adminUid = request.auth.uid;
        const { tutorId } = request.data;
        if (!tutorId || typeof tutorId !== "string") {
            throw new HttpsError("invalid-argument", "Invalid tutorId provided.");
        }
        functions.logger.info(`Admin (${adminUid}) approving tutor (${tutorId}).`);
        try {
            const tutorRef = db.collection("users").doc(tutorId); // Uses global db
            await tutorRef.update({
                profileStatus: "verified",
                rejectionReason: admin.firestore.FieldValue.delete(),
                approvedBy: adminUid,
                approvedAt: admin.firestore.FieldValue.serverTimestamp()
            });
            functions.logger.info(`Tutor (${tutorId}) approved by Admin (${adminUid}).`);
            return { success: true, message: "Tutor approved." };
        } catch (error: any) {
             functions.logger.error(`Error approving tutor (${tutorId}) by Admin (${adminUid}):`, error);
             if (error instanceof HttpsError) throw error;
             throw new HttpsError("internal", "Failed to update tutor status.", error.message);
        }
    }
);

// ---- Function 4: rejectTutor ----
export const rejectTutor = onCall<AdminTutorActionData>(
    async (request: CallableRequest<AdminTutorActionData>) => {
        if (request.auth?.token?.isAdmin !== true) {
            functions.logger.warn("rejectTutor called by non-admin.", { uid: request.auth?.uid });
            throw new HttpsError("permission-denied", "Admin privileges required.");
        }
        const adminUid = request.auth.uid;
        const { tutorId, reason } = request.data;
        if (!tutorId || typeof tutorId !== "string") {
            throw new HttpsError("invalid-argument", "Invalid tutorId provided.");
        }
        if (reason !== undefined && (typeof reason !== "string" || reason.trim() === "")) {
            throw new HttpsError("invalid-argument", "Reason must be a non-empty string if provided.");
        }
        functions.logger.info(`Admin (${adminUid}) rejecting tutor (${tutorId}). Reason: ${reason || "None"}`);
        try {
            const tutorRef = db.collection("users").doc(tutorId); // Uses global db
            const updateData: { profileStatus: string; rejectionReason?: string | FieldValue; rejectedBy?: string; rejectedAt?: FieldValue; } = {
                profileStatus: "rejected", rejectedBy: adminUid, rejectedAt: admin.firestore.FieldValue.serverTimestamp()
            };
            updateData.rejectionReason = (reason && reason.trim()) ? reason.trim() : admin.firestore.FieldValue.delete();
            await tutorRef.update(updateData);
            functions.logger.info(`Tutor (${tutorId}) rejected by Admin (${adminUid}).`);
            return { success: true, message: "Tutor rejected." };
        } catch (error: any) {
            functions.logger.error(`Error rejecting tutor (${tutorId}) by Admin (${adminUid}):`, error);
            if (error instanceof HttpsError) throw error;
            throw new HttpsError("internal", "Failed to update tutor status.", error.message);
        }
    }
);

// ---- Function 5: blockUser ----
export const blockUser = onCall<AdminUserActionData>(
    async (request: CallableRequest<AdminUserActionData>) => {
        if (request.auth?.token?.isAdmin !== true) {
            functions.logger.warn("blockUser called by non-admin.", { uid: request.auth?.uid });
            throw new HttpsError("permission-denied", "Admin privileges required.");
        }
        const adminUid = request.auth.uid;
        const { userId } = request.data;
        if (!userId || typeof userId !== "string") {
            throw new HttpsError("invalid-argument", "Invalid userId provided.");
        }
        if (userId === adminUid) {
            throw new HttpsError("failed-precondition", "Admin cannot block themselves.");
        }
        functions.logger.info(`Admin (${adminUid}) blocking user (${userId}).`);
        try {
            await admin.auth().updateUser(userId, { disabled: true });
            await db.collection("users").doc(userId).update({ // Uses global db
                isBlocked: true,
                blockedBy: adminUid,
                blockedAt: admin.firestore.FieldValue.serverTimestamp()
            });
            functions.logger.info(`User (${userId}) blocked by Admin (${adminUid}) in Auth and Firestore.`);
            return { success: true, message: "User blocked." };
        } catch (error: any) {
            functions.logger.error(`Error blocking user (${userId}) by Admin (${adminUid}):`, error);
            if (error instanceof HttpsError) throw error;
            if ((error as any).code === 'auth/user-not-found') {
                functions.logger.warn(`User ${userId} not found in Firebase Auth during block attempt.`);
                throw new HttpsError("not-found", `User ${userId} not found in authentication system.`);
            }
            throw new HttpsError("internal", "Failed to block user.", error.message);
        }
    }
);

// ---- Function 6: unblockUser ----
export const unblockUser = onCall<AdminUserActionData>(
    async (request: CallableRequest<AdminUserActionData>) => {
        if (request.auth?.token?.isAdmin !== true) {
            functions.logger.warn("unblockUser called by non-admin.", { uid: request.auth?.uid });
            throw new HttpsError("permission-denied", "Admin privileges required.");
        }
        const adminUid = request.auth.uid;
        const { userId } = request.data;
        if (!userId || typeof userId !== "string") {
            throw new HttpsError("invalid-argument", "Invalid userId provided.");
        }
        functions.logger.info(`Admin (${adminUid}) unblocking user (${userId}).`);
        try {
            await admin.auth().updateUser(userId, { disabled: false });
            await db.collection("users").doc(userId).update({ isBlocked: false }); // Uses global db
            functions.logger.info(`User (${userId}) unblocked by Admin (${adminUid}) in Auth and Firestore.`);
            return { success: true, message: "User unblocked." };
        } catch (error: any) {
            functions.logger.error(`Error unblocking user (${userId}) by Admin (${adminUid}):`, error);
            if (error instanceof HttpsError) throw error;
            if ((error as any).code === 'auth/user-not-found') {
                functions.logger.warn(`User ${userId} not found in Firebase Auth during unblock attempt.`);
                try { await db.collection("users").doc(userId).update({ isBlocked: false }); } catch (fsError) {} // Uses global db
                throw new HttpsError("not-found", `User ${userId} not found in authentication system.`);
            }
            throw new HttpsError("internal", "Failed to unblock user.", error.message);
        }
    }
);

// ---- Lazy Initializer for Firestore (specifically for getDashboardStats) ----
// Defined before its use in getDashboardStats
let firestoreForStatsInstance: admin.firestore.Firestore | undefined;
function getFirestoreForStats(): admin.firestore.Firestore {
  if (!firestoreForStatsInstance) {
    functions.logger.info("Initializing Firestore instance LAZILY specifically for getDashboardStats...");
    firestoreForStatsInstance = admin.firestore();
    functions.logger.info("Firestore instance for getDashboardStats initialized LAZILY.");
  }
  return firestoreForStatsInstance;
}

// ---- Function 7: getDashboardStats ----
export const getDashboardStats = onCall(
    async (request: CallableRequest<any>) => {
        const firestoreDB = getFirestoreForStats(); // Use the specific lazy getter

        if (request.auth?.token?.isAdmin !== true) {
            functions.logger.warn("getDashboardStats called by non-admin.", { uid: request.auth?.uid });
            throw new HttpsError("permission-denied", "Admin privileges required.");
        }
        const adminUid = request.auth.uid;
        functions.logger.info(`Admin (${adminUid}) requesting dashboard stats via specific DB instance.`);

        try {
            const usersRef = firestoreDB.collection("users");
            const bookingsRef = firestoreDB.collection("bookings");

            const thirtyDaysAgo = new Date();
            thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
            const thirtyDaysAgoTimestamp = Timestamp.fromDate(thirtyDaysAgo);

            const totalRegisteredTutorsQuery = usersRef.where("role", "==", "Tutor").count().get();
            const verifiedTutorsQuery = usersRef.where("role", "==", "Tutor").where("profileStatus", "==", "verified").count().get();
            const pendingVerificationTutorsQuery = usersRef.where("role", "==", "Tutor").where("profileStatus", "==", "pending_verification").count().get();
            const rejectedTutorsQuery = usersRef.where("role", "==", "Tutor").where("profileStatus", "==", "rejected").count().get();
            const totalTuteesQuery = usersRef.where("role", "==", "Tutee").count().get();
            const activeUsersQuery = usersRef.where("fcmTokenLastUpdated", ">=", thirtyDaysAgoTimestamp).count().get();
            const totalBookingsQuery = bookingsRef.count().get();
            const confirmedBookingsQuery = bookingsRef.where("bookingStatus", "==", "confirmed").count().get();

            const [
                totalRegisteredTutorsSnap,
                verifiedTutorsSnap,
                pendingVerificationTutorsSnap,
                rejectedTutorsSnap,
                totalTuteesSnap,
                activeUsersSnap,
                totalBookingsSnap,
                confirmedBookingsSnap,
            ] = await Promise.all([
                totalRegisteredTutorsQuery,
                verifiedTutorsQuery,
                pendingVerificationTutorsQuery,
                rejectedTutorsQuery,
                totalTuteesQuery,
                activeUsersQuery,
                totalBookingsQuery,
                confirmedBookingsQuery,
            ]);

            const stats = {
                totalRegisteredTutors: totalRegisteredTutorsSnap.data().count,
                totalVerifiedTutors: verifiedTutorsSnap.data().count,
                totalPendingTutors: pendingVerificationTutorsSnap.data().count,
                totalRejectedTutors: rejectedTutorsSnap.data().count,
                totalTutees: totalTuteesSnap.data().count,
                totalActiveUsersLast30Days: activeUsersSnap.data().count,
                totalBookings: totalBookingsSnap.data().count,
                totalConfirmedBookings: confirmedBookingsSnap.data().count,
            };

            functions.logger.info(`Dashboard stats calculated for Admin (${adminUid}):`, stats);
            return { success: true, stats: stats };

        } catch (error: any) {
            functions.logger.error(`Error getting dashboard stats for Admin (${adminUid}):`, error);
            if (error instanceof HttpsError) throw error;
            const errorMessage = error.message ? error.message : "An unknown error occurred.";
            throw new HttpsError("internal", "Failed to calculate dashboard statistics.", errorMessage);
        }
    }
);
// ---- End getDashboardStats Function ----

// ---- Scheduled Function: updateCompletedBookings ----
export const updateCompletedBookings = onSchedule(
  {
    schedule: "every 60 minutes",
    timeZone: "Africa/Johannesburg",
  },
  async (event: ScheduledEvent) => {
    // This function uses the global `db`
    functions.logger.info( `Scheduled: updateCompletedBookings running at ${event.scheduleTime}`, { structuredData: true } );
    const now = Timestamp.now();
    const bookingsRef = db.collection("bookings");

    try {
      const querySnapshot = await bookingsRef
        .where("bookingStatus", "==", "confirmed")
        .where("endTime", "<=", now)
        .get();

      if (querySnapshot.empty) {
        functions.logger.info("Scheduled: No confirmed bookings found past their end time.", { structuredData: true });
        return;
      }
      const batch = db.batch(); // Uses global db for batch
      let updateCount = 0;
      querySnapshot.forEach((doc) => {
        functions.logger.info(`Scheduled: Updating booking ${doc.id} to 'completed'.`);
        batch.update(doc.ref, {
            bookingStatus: "completed",
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        updateCount++;
      });
      await batch.commit();
      functions.logger.info(`Scheduled: Successfully updated ${updateCount} bookings to 'completed'.`, { structuredData: true });
    } catch (error) {
      functions.logger.error("Scheduled: Error in updateCompletedBookings:", error, { structuredData: true });
    }
  }
);
// ---- END Scheduled Function ----

// ---- Firestore Trigger: Send Booking Notification to Tutor ----
export const sendBookingNotificationToTutor = onDocumentCreated(
  "bookings/{bookingId}",
  async (event: FirestoreEvent<QueryDocumentSnapshot | undefined, { bookingId: string }>) => {
    // This function uses the global `db`
    const snapshot = event.data;
    if (!snapshot) {
      functions.logger.log("Booking notification trigger: No data associated with the event. Exiting.");
      return;
    }
    const bookingData = snapshot.data();
    const bookingId = event.params.bookingId;
    functions.logger.log(`Booking Trigger: New booking detected - ${bookingId}`, { bookingData });
    const tutorUid = bookingData.tutorUid;
    const tuteeName = bookingData.tuteeName || "A tutee";
    const moduleCode = bookingData.moduleCode || "a session";
    const startTime = bookingData.startTime;

    if (!tutorUid || typeof tutorUid !== 'string') {
      functions.logger.error(`Booking Trigger (${bookingId}): Missing or invalid tutorUid. Cannot send notification.`);
      return;
    }
    if (!(startTime instanceof Timestamp)) {
      functions.logger.error(`Booking Trigger (${bookingId}): startTime is not a valid Timestamp. Cannot format time.`, { startTimeRaw: startTime });
    }
    let sessionDetails = "at the scheduled time";
    if (startTime instanceof Timestamp) {
        try {
            const date = startTime.toDate();
            sessionDetails = `on ${date.toLocaleDateString('en-ZA', { year: 'numeric', month: 'long', day: 'numeric', timeZone: 'Africa/Johannesburg' })} at ${date.toLocaleTimeString('en-ZA', { hour: '2-digit', minute: '2-digit', timeZone: 'Africa/Johannesburg', hour12: true })}`;
        } catch (e) {
            functions.logger.error(`Booking Trigger (${bookingId}): Error formatting startTime:`, e);
            sessionDetails = "at the scheduled time";
        }
    } else {
        sessionDetails = "details to be confirmed (time issue)";
    }
    functions.logger.log(`Booking Trigger (${bookingId}): Preparing FCM notification for Tutor ${tutorUid}`);
    let fcmToken: string | null = null;
    try {
      const tutorDocSnap = await db.collection("users").doc(tutorUid).get(); // Uses global db
      if (tutorDocSnap.exists) {
        fcmToken = tutorDocSnap.data()?.fcmToken || null;
      } else {
        functions.logger.warn(`Booking Trigger (${bookingId}): Tutor document users/${tutorUid} not found.`);
        return;
      }
    } catch (error) {
      functions.logger.error(`Booking Trigger (${bookingId}): Error fetching tutor document users/${tutorUid}:`, error);
      return;
    }
    if (!fcmToken || typeof fcmToken !== 'string' || fcmToken.trim() === '') {
      functions.logger.log(`Booking Trigger (${bookingId}): Tutor ${tutorUid} has no valid FCM token. Skipping notification.`);
      return;
    }
    const payload: admin.messaging.Message = {
      notification: { title: "New Booking Received!", body: `${tuteeName} booked you for ${moduleCode} ${sessionDetails}.` },
      data: { bookingId: bookingId, notificationType: "NEW_BOOKING_TUTOR", tutorUid: tutorUid },
      token: fcmToken, android: { priority: "high" as const }, apns: { payload: { aps: { sound: "default", contentAvailable: true } } },
    };
    functions.logger.log(`Booking Trigger (${bookingId}): Sending FCM to Tutor ${tutorUid}`, { payload });
    try {
      const response = await admin.messaging().send(payload);
      functions.logger.log(`Booking Trigger (${bookingId}): Successfully sent FCM message:`, response);
    } catch (error: any) {
      functions.logger.error(`Booking Trigger (${bookingId}): Error sending FCM message to Tutor ${tutorUid}:`, error);
      const errorCode = (error as admin.FirebaseError)?.code;
      if (errorCode === "messaging/registration-token-not-registered" || errorCode === "messaging/invalid-registration-token") {
        functions.logger.warn(`Booking Trigger (${bookingId}): Invalid FCM token for Tutor ${tutorUid}. Consider removing from Firestore.`);
        try {
          await db.collection("users").doc(tutorUid).update({ fcmToken: admin.firestore.FieldValue.delete() }); // Uses global db
          functions.logger.log(`Booking Trigger (${bookingId}): Successfully removed invalid FCM token for Tutor ${tutorUid}.`);
        } catch (updateError) {
          functions.logger.error(`Booking Trigger (${bookingId}): Failed to delete invalid FCM token for Tutor ${tutorUid}:`, updateError);
        }
      }
    }
  }
);

// ---- Firestore Trigger: Send Chat Message Notification ----
export const sendChatMessageNotification = onDocumentCreated(
  "chat_rooms/{chatRoomId}/messages/{messageId}",
  async (event: FirestoreEvent<QueryDocumentSnapshot | undefined, { chatRoomId: string; messageId: string }>) => {
    // This function uses the global `db`
    const snapshot = event.data;
    if (!snapshot) {
      functions.logger.log("Chat Message Trigger: No data associated with the event. Exiting.");
      return;
    }
    const messageData = snapshot.data();
    const chatRoomId = event.params.chatRoomId;
    const messageId = event.params.messageId;
    functions.logger.log(`Chat Message Trigger: New message ${messageId} in chatRoom ${chatRoomId}`, { messageData });
    const senderId = messageData.senderId;
    const messageText = messageData.text || "Sent you a message";

    if (!senderId || typeof senderId !== "string") {
      functions.logger.error(`Chat Message Trigger (${chatRoomId}/${messageId}): Message data missing or invalid senderId.`);
      return;
    }
    let receiverId: string | undefined;
    if (messageData.receiverId && typeof messageData.receiverId === "string") {
      receiverId = messageData.receiverId;
      functions.logger.log(`Chat Message Trigger (${chatRoomId}/${messageId}): ReceiverId found in message data: ${receiverId}`);
    } else {
      functions.logger.log(`Chat Message Trigger (${chatRoomId}/${messageId}): ReceiverId not in message. Fetching chat_rooms doc.`);
      try {
        const chatRoomDocSnap = await db.collection("chat_rooms").doc(chatRoomId).get(); // Uses global db
        if (chatRoomDocSnap.exists) {
          const chatRoomData = chatRoomDocSnap.data();
          const participantIds = chatRoomData?.participantIds as string[];
          if (Array.isArray(participantIds) && participantIds.length === 2) {
            receiverId = participantIds.find((id) => id !== senderId);
            if (receiverId) {
                functions.logger.log(`Chat Message Trigger (${chatRoomId}/${messageId}): ReceiverId determined from participants: ${receiverId}`);
            } else {
                 functions.logger.error(`Chat Message Trigger (${chatRoomId}/${messageId}): Could not find a different participant in participantIds. senderId: ${senderId}, participants: ${participantIds.join(", ")}`);
            }
          } else {
            functions.logger.error(`Chat Message Trigger (${chatRoomId}/${messageId}): 'participantIds' field is missing, not an array, or not of length 2 in chat_room: ${chatRoomId}.`);
            return;
          }
        } else {
          functions.logger.error(`Chat Message Trigger (${chatRoomId}/${messageId}): Chat room document chat_rooms/${chatRoomId} not found.`);
          return;
        }
      } catch (error) {
        functions.logger.error(`Chat Message Trigger (${chatRoomId}/${messageId}): Error fetching chat room document chat_rooms/${chatRoomId}:`, error);
        return;
      }
    }
    if (!receiverId) {
      functions.logger.error(`Chat Message Trigger (${chatRoomId}/${messageId}): Could not determine receiverId. senderId: ${senderId}.`);
      return;
    }
    if (senderId === receiverId) {
      functions.logger.log(`Chat Message Trigger (${chatRoomId}/${messageId}): Sender ${senderId} and receiver ${receiverId} are the same. Skipping notification.`);
      return;
    }
    let senderName = "Someone";
    try {
      const senderDocSnap = await db.collection("users").doc(senderId).get(); // Uses global db
      if (senderDocSnap.exists) {
        const senderProfile = senderDocSnap.data();
        senderName = senderProfile?.name || `${senderProfile?.firstName || ""} ${senderProfile?.surname || ""}`.trim() || "Someone";
      } else {
         functions.logger.warn(`Chat Message Trigger (${chatRoomId}/${messageId}): Sender document users/${senderId} not found. Using default name.`);
      }
    } catch (error) {
      functions.logger.error(`Chat Message Trigger (${chatRoomId}/${messageId}): Error fetching sender's profile users/${senderId}:`, error);
    }
    let fcmToken: string | null = null;
    try {
      const receiverDocSnap = await db.collection("users").doc(receiverId).get(); // Uses global db
      if (receiverDocSnap.exists) {
        fcmToken = receiverDocSnap.data()?.fcmToken || null;
      } else {
        functions.logger.warn(`Chat Message Trigger (${chatRoomId}/${messageId}): Receiver document users/${receiverId} not found.`);
        return;
      }
    } catch (error) {
      functions.logger.error(`Chat Message Trigger (${chatRoomId}/${messageId}): Error fetching receiver's profile users/${receiverId}:`, error);
      return;
    }
    if (!fcmToken || typeof fcmToken !== 'string' || fcmToken.trim() === '') {
      functions.logger.log(`Chat Message Trigger (${chatRoomId}/${messageId}): Receiver ${receiverId} has no valid FCM token. Skipping notification.`);
      return;
    }
    const payload: admin.messaging.Message = {
      notification: { title: `New message from ${senderName}`, body: messageText.length > 100 ? messageText.substring(0, 97) + "..." : messageText, },
      data: { type: "CHAT_MESSAGE", chatRoomId: chatRoomId, senderId: senderId, senderName: senderName },
      token: fcmToken, android: { priority: "high" as const }, apns: { payload: { aps: { sound: "default", badge: 1 } } },
    };
    functions.logger.log(`Chat Message Trigger (${chatRoomId}/${messageId}): Sending FCM to Receiver ${receiverId}`, { payload });
    try {
      const response = await admin.messaging().send(payload);
      functions.logger.log(`Chat Message Trigger (${chatRoomId}/${messageId}): Successfully sent FCM message to ${receiverId}:`, response);
    } catch (error: any) {
      functions.logger.error(`Chat Message Trigger (${chatRoomId}/${messageId}): Error sending FCM message to Receiver ${receiverId}:`, error);
      const errorCode = (error as admin.FirebaseError)?.code;
      if (errorCode === "messaging/registration-token-not-registered" || errorCode === "messaging/invalid-registration-token") {
        functions.logger.warn(`Chat Message Trigger (${chatRoomId}/${messageId}): Invalid FCM token for Receiver ${receiverId}. Removing from Firestore.`);
        try {
          await db.collection("users").doc(receiverId).update({ fcmToken: admin.firestore.FieldValue.delete() }); // Uses global db
           functions.logger.log(`Chat Message Trigger (${chatRoomId}/${messageId}): Successfully removed invalid FCM token for Receiver ${receiverId}.`);
        } catch (updateError) {
          functions.logger.error(`Chat Message Trigger (${chatRoomId}/${messageId}): Failed to delete invalid FCM token for Receiver ${receiverId}:`, updateError);
        }
      }
    }
  }
);
// ** Ensure no characters or lines after this final comment **