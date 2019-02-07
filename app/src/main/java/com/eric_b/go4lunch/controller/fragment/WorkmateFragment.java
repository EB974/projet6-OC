package com.eric_b.go4lunch.controller.fragment;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.bumptech.glide.Glide;
import com.eric_b.go4lunch.R;
import com.eric_b.go4lunch.View.WorkmateAdapter;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Objects;

import com.eric_b.go4lunch.utils.SPAdapter;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import javax.annotation.Nullable;



public class WorkmateFragment extends Fragment {

    private ListView mListView;
    private static final String RESTAURANT_NAME = "restaurant_name";
    private String mRestaurantName;

    public WorkmateFragment() {
        // Required empty public constructor
    }

    public static WorkmateFragment newInstance(String name) {
        WorkmateFragment fragment = new WorkmateFragment();
        Bundle args = new Bundle();
        args.putString(RESTAURANT_NAME, name);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mRestaurantName = getArguments().getString(RESTAURANT_NAME);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_workmate, container, false);
        mListView = view.findViewById(R.id.workmate_fragment_listview);
        // call the method setHasOptionsMenu, to have access to the menu from your fragment
        setHasOptionsMenu(true);
        getWorkmateList();
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.findItem(R.id.menu_cancel).setVisible(false);
        if (mRestaurantName!=null)
            menu.findItem(R.id.menu_cancel).setVisible(true);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void getWorkmateList() {
        final ArrayList<String> workmateList = new ArrayList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final CollectionReference colRef = db.collection("compagny1");
        colRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                    ListIterator<DocumentSnapshot> data = queryDocumentSnapshots.getDocuments().listIterator();
                    workmateList.clear();

                    for (; data.hasNext(); ) {
                        DocumentSnapshot element = data.next();
                        String uid = (String) element.get("uid");
                        if(mRestaurantName==null){
                            workmateList.add(uid);
                        }else{
                            if(Objects.equals(element.get("reservedRestaurantName"), mRestaurantName))
                            workmateList.add(uid);
                        }

                    }
                    SPAdapter spAdapter = new SPAdapter(Objects.requireNonNull(getActivity()));
                    String userId = spAdapter.getUserId();
                    configureListView(workmateList,userId);

                }
            }

        });

    }


    private void configureListView(ArrayList<String> IdList, String userId){
        WorkmateAdapter adapter = new WorkmateAdapter(getActivity(), IdList, Glide.with(this),userId);
        mListView.setAdapter(adapter);

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
