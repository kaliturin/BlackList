package com.kaliturin.blacklist;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 */
public class AddContactManualFragment extends Fragment {
    // bundle argument name
    public static final String CONTACT_TYPE = "CONTACT_TYPE";
    private int contactType = 0;
    private SnackBarCustom snackBar = null;

    public AddContactManualFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        contactType = bundle.getInt(CONTACT_TYPE);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_contact_manual, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // snack bar
        snackBar = new SnackBarCustom(view, R.id.snack_bar);
        // "Add" button
        snackBar.setButton(R.id.button_left,
                getString(R.string.add),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });

        // "Cancel button" button
        snackBar.setButton(R.id.button_right,
                getString(R.string.cancel),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getActivity().finish();
                    }
                });

        snackBar.show();
    }
}
