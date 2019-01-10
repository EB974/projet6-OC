package com.eric_b.go4lunch.controller.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

import com.bumptech.glide.Glide;
import com.eric_b.go4lunch.R;
import com.eric_b.go4lunch.Utils.SPAdapter;
import com.eric_b.go4lunch.View.ListViewAdapter;
import com.eric_b.go4lunch.View.WorkmateAdapter;
import com.firebase.ui.auth.AuthUI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import com.eric_b.go4lunch.LogInActivity;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import javax.annotation.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;

public class WorkmateFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private WorkmateAdapter mAdapter;
    private ListView mListView;

    public WorkmateFragment() {
        // Required empty public constructor
    }

    public static WorkmateFragment newInstance(String param1, String param2) {
        WorkmateFragment fragment = new WorkmateFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_workmate, container, false);
        mListView = view.findViewById(R.id.workmate_fragment_listview);
        getWorkmateList();
        return view;
    }

    private void getWorkmateList() {
        final ArrayList workmateList = new ArrayList();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final CollectionReference colRef = db.collection("compagny1");
        colRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                    //mReserved = documentSnapshot.getBoolean("reserved");
                    ListIterator<DocumentSnapshot> data = queryDocumentSnapshots.getDocuments().listIterator();
                    workmateList.clear();
                    for (; data.hasNext(); ) {
                        DocumentSnapshot element = data.next();
                        String uid = (String) element.get("uid");
                        workmateList.add(uid);
                    }
                    SPAdapter spAdapter = new SPAdapter(getContext());
                    String userId = spAdapter.getUserId();
                    configureListView(workmateList,userId);

                }
            }

        });

    }


    private void configureListView(ArrayList IdList, String UserId){
        this.mAdapter = new WorkmateAdapter(getActivity(),IdList,Glide.with(this), UserId);
        mListView.setAdapter(mAdapter);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
