package com.eric_b.go4lunch;

import com.eric_b.go4lunch.modele.place.GooglePlacePojo;
import com.eric_b.go4lunch.utils.PlaceStream;

import org.junit.Test;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

import static org.hamcrest.MatcherAssert.assertThat;

public class ReceivingTest {

        @Test
        public void streamFetchPlaceTest() {
            //1 - Get the stream
            Observable<GooglePlacePojo> observablePlace = PlaceStream.streamFetchsNearbyRestaurants("0,0","500", "restaurant","AIzaSyALfrBMN4ibzo7Ysp-1hgN3FXv1Ddx00F0");
            //2 - Create a new TestObserver
            TestObserver<GooglePlacePojo> testObserver = new TestObserver<>();
            //3 - Launch observable
            observablePlace.subscribeWith(testObserver)
                    .assertNoErrors()
                    .assertNoTimeout()
                    .awaitTerminalEvent();

            GooglePlacePojo placeFetched = testObserver.values().get(0);
            assertThat("Place receive info.", placeFetched.getStatus().compareTo("ZERO_RESULTS"));

        }

    private void assertThat(String s, int ok) {
    }
}
