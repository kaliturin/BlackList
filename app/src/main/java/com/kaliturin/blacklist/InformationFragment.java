package com.kaliturin.blacklist;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * Information fragment
 */
public class InformationFragment extends Fragment implements FragmentArguments {
    private static final String LIST_POSITION = "LIST_POSITION";
    private InformationArrayAdapter adapter = null;
    private ListView listView = null;
    private int listPosition = 0;

    public InformationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // set activity title
        Bundle arguments = getArguments();
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (arguments != null && actionBar != null) {
            actionBar.setTitle(arguments.getString(TITLE));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            listPosition = savedInstanceState.getInt(LIST_POSITION, 0);
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_information, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listView = (ListView) view.findViewById(R.id.help_list);

        adapter = new InformationArrayAdapter(getContext());

        adapter.addTitle(R.string.Black_list);
        adapter.addButton(R.string.Black_list, R.string.Help_black_list);
        adapter.addButton(R.string.White_list, R.string.Help_white_list);

        listView.setAdapter(adapter);
        listView.setSelection(listPosition);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save first showed row position
        outState.putInt(LIST_POSITION, listView.getFirstVisiblePosition());
    }


}
