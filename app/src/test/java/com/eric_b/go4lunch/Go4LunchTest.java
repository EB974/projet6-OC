package com.eric_b.go4lunch;

import android.location.Location;

import com.eric_b.go4lunch.utils.CountStar;
import com.eric_b.go4lunch.utils.Distance;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class Go4LunchTest {
    @Mock
    Location loc1  =  new Location("testprovider1");
    @Mock
    Location loc2  =  new Location("testprovider2");
    @Mock
    Distance distance = new Distance(loc1,loc2);

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void test_distance() {

        loc1.setLatitude(20.3);
        loc1.setLongitude(52.6);
        loc2.setLatitude(20.3);
        loc2.setLongitude(52.6);

        int dist = distance.getDistance();
        //when(mockedDistance).thenReturn(d);

        //verify(mockedDistance(loc1,loc2)).getDistance();
        assertEquals(0,dist);
        verify(distance).getDistance();
    }



    @Test
    public void test_CountStar() {
        assertEquals(2,new CountStar(36,18).getcount());
    }

}