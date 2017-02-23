package com.kaliturin.blacklist;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class SendSMSFragment extends Fragment implements FragmentArguments {
    public SendSMSFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_send_sms, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle arguments = getArguments();
        if(arguments != null) {
            String person = arguments.getString(PERSON);
            String number = arguments.getString(NUMBER);
            EditText editText = (EditText) view.findViewById(R.id.edit_person);
            editText.setText(person);
            editText = (EditText) view.findViewById(R.id.edit_number);
            editText.setText(number);
        }
    }
}
