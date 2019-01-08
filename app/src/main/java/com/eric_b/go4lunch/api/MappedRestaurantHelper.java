package com.eric_b.go4lunch.api;

import com.eric_b.go4lunch.modele.MappedRestaurant;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class MappedRestaurantHelper {

    private static final String COLLECTION_NAME = "mappedRestaurant";

    // --- COLLECTION REFERENCE ---
    public static CollectionReference getMappedRestaurantCollection(){
        return FirebaseFirestore.getInstance().collection(COLLECTION_NAME);
    }

    // --- CREATE ---
    public static Task<Void> createRestaurant(String restaurantPlaceId, Boolean reserved, int numberOfStar, int numberOfRating, String numberOfWorkmate, HashMap<String,HashMap<String,String>> workmatesReservation) {
        MappedRestaurant restaurantToCreate = new MappedRestaurant(reserved, numberOfStar, numberOfRating, numberOfWorkmate, workmatesReservation);
        return MappedRestaurantHelper.getMappedRestaurantCollection().document(restaurantPlaceId).set(restaurantToCreate);
    }


    // --- GET ---

    public static Task<DocumentSnapshot> getRestaurant(String restaurantPlaceId){
        return MappedRestaurantHelper.getMappedRestaurantCollection().document(restaurantPlaceId).get();
    }

    // --- UPDATE ---

    public static Task<Void> updateRestaurantReserved(String restaurantPlaceId, boolean reserved) {
        return MappedRestaurantHelper.getMappedRestaurantCollection().document(restaurantPlaceId).update("reserved", reserved);
    }

    public static Task<Void> updateRestaurantNumberOfStar(String restaurantPlaceId, int numberOfStar) {
        return MappedRestaurantHelper.getMappedRestaurantCollection().document(restaurantPlaceId).update("numberOfStar", numberOfStar);
    }

    public static Task<Void> updateRestaurantNumberOfRating(String restaurantPlaceId, int numberOfRating) {
        return MappedRestaurantHelper.getMappedRestaurantCollection().document(restaurantPlaceId).update("numberOfRating", numberOfRating);
    }

    public static Task<Void> updateRestaurantNumberOfWorkmate(String restaurantPlaceId, String numberOfWorkmate) {
        return MappedRestaurantHelper.getMappedRestaurantCollection().document(restaurantPlaceId).update("numberOfWorkmate", numberOfWorkmate);
    }

    public static Task<Void> updateRestaurantWorkmateList(String restaurantPlaceId, HashMap<String,HashMap<String,String>> workmateList) {
        return MappedRestaurantHelper.getMappedRestaurantCollection().document(restaurantPlaceId).update("workmatesReservation", workmateList);
    }


    // --- DELETE ---

    public static Task<Void> deleteRestaurantId(String restaurantPlaceId) {
        return MappedRestaurantHelper.getMappedRestaurantCollection().document(restaurantPlaceId).delete();
    }


}
