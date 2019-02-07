package com.eric_b.go4lunch.api;

import com.eric_b.go4lunch.modele.Compagny;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class CompagnyHelper {

    private static final String COLLECTION_NAME = "compagny1";

    // --- COLLECTION REFERENCE ---
    private static CollectionReference getWorkmateCollection() {
        return FirebaseFirestore.getInstance().collection(COLLECTION_NAME);
    }

    // --- CREATE ---
    public static Task<Void> createWorkmate(String uid, String workmatename, String urlPicture, String restaurantPlaceId, String restaurantName) {
        Compagny workmateToCreate = new Compagny(uid, workmatename, urlPicture, restaurantPlaceId, restaurantName);
        return CompagnyHelper.getWorkmateCollection().document(uid).set(workmateToCreate);
    }


    // --- GET ---

    public static Task<DocumentSnapshot> getWorkmate(String uid) {
        return CompagnyHelper.getWorkmateCollection().document(uid).get();
    }


    // --- UPDATE ---

    public static Task<Void> updateWorkmatename(String workmatename, String uid) {
        return CompagnyHelper.getWorkmateCollection().document(uid).update("workmateName", workmatename);
    }

    public static Task<Void> updateWorkmateRestaurant(String restaurantId, String uid) {
        return CompagnyHelper.getWorkmateCollection().document(uid).update("reservedRestaurant", restaurantId);
    }

    public static Task<Void> updateWorkmateRestaurantName(String restaurantName, String uid) {
        return CompagnyHelper.getWorkmateCollection().document(uid).update("reservedRestaurantName", restaurantName);
    }


    public static Task<Void> updateWorkmateImage(String urlPhoto, String uid) {
        return CompagnyHelper.getWorkmateCollection().document(uid).update("urlPhoto", urlPhoto);
    }


    // --- DELETE ---

    public static Task<Void> deleteWorkmate(String uid) {
        return CompagnyHelper.getWorkmateCollection().document(uid).delete();
    }
}