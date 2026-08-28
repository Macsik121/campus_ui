package com.sfedu.campus.squad_log;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.sfedu.campus.R;
import com.sfedu.campus.data.datasource.DataCallback;
import com.sfedu.campus.generated.model.Child;
import com.sfedu.campus.generated.model.Event;
import com.sfedu.campus.generated.model.GetAttendanceResponse;
import com.sfedu.campus.generated.model.GetSquadChildrenResponse;
import com.sfedu.campus.generated.model.UpdateAttendanceResponse;
import com.sfedu.campus.helpers.PreferencesHelper;
import com.sfedu.campus.helpers.ViewUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SquadLogFragment extends Fragment
        implements ChildAttendanceAdapter.OnAttendanceChangeListener, EventAdapter.OnEventClickListener {

    private static final String TAG = "SquadLogFragment";
    private static final String ARG_SQUAD_ID = "squad_id";

    // UI Elements
    private MaterialAutoCompleteTextView datePicker;
    private ImageView calendarIcon;
    private TextView attendanceCountText;
    private RecyclerView childrenRecyclerView;
    private RecyclerView eventsRecyclerView;
    private ProgressBar progressBar;
    private View loadingOverlay;

    // Data
    private SquadLogRepository repository;
    private PreferencesHelper preferencesHelper;
    private ChildAttendanceAdapter childAdapter;
    private EventAdapter eventAdapter;
    private UUID squadId;
    private LocalDate selectedDate;
    private boolean isLoading = false;
    private UUID selectedEventId = null;

    public SquadLogFragment() {
        // Required empty public constructor
    }

    public static SquadLogFragment newInstance(UUID squadId) {
        SquadLogFragment fragment = new SquadLogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SQUAD_ID, squadId.toString());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String squadIdStr = getArguments().getString(ARG_SQUAD_ID);
            if (squadIdStr != null) {
                squadId = UUID.fromString(squadIdStr);
            }
        }
        preferencesHelper = new PreferencesHelper(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_squad_log, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI elements
        datePicker = view.findViewById(R.id.date_picker);
        calendarIcon = view.findViewById(R.id.calendar_icon);
        attendanceCountText = view.findViewById(R.id.attendance_count_text);
        childrenRecyclerView = view.findViewById(R.id.children_recycler_view);
        eventsRecyclerView = view.findViewById(R.id.events_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        loadingOverlay = view.findViewById(R.id.loading_overlay);
        LinearLayout llSquadLogDatePicker = view.findViewById(R.id.ll_squad_log_date_picker);

        datePicker.setEnabled(false);
        // Initialize repository
        repository = new SquadLogRepository(requireContext());

        // Setup date picker
        setupDatePicker();

        // Setup children RecyclerView
        childAdapter = new ChildAttendanceAdapter();
        childAdapter.setOnAttendanceChangeListener(this);
        childrenRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        childrenRecyclerView.setAdapter(childAdapter);

        // Setup events RecyclerView
        eventAdapter = new EventAdapter();
        eventAdapter.setOnEventClickListener(this);
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        eventsRecyclerView.setAdapter(eventAdapter);

        // Setup calendar icon click
        llSquadLogDatePicker.setOnClickListener(v -> datePicker.showDropDown());

        // Load initial data
        loadChildren();
        loadEventsForCurrentDate();
    }

    private void setupDatePicker() {
        // Generate date options for the dropdown (last 30 days + next 30 days)
        List<String> dateOptions = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = -30; i <= 30; i++) {
            LocalDate date = today.plusDays(i);
            dateOptions.add(date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                dateOptions
        );
        datePicker.setAdapter(adapter);

        // Set current date as default
        selectedDate = today;
        datePicker.setText(selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), false);

        // Handle date selection
        datePicker.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            try {
                selectedDate = LocalDate.parse(selected, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                loadEventsForDate(selectedDate);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing date: " + e.getMessage());
            }
        });
    }

    private void loadChildren() {
        squadId = preferencesHelper.getSquadId();
        if (squadId == null) return;
        Log.i("SquadLogFragment", "loadChildren: squadId=" + squadId.toString() + ", isLoading=" + isLoading);

        setLoading(true);
        repository.getChildren(squadId, new DataCallback<GetSquadChildrenResponse>() {
            @Override
            public void onSuccess(GetSquadChildrenResponse response) {
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    if (response != null && response.getChildren() != null) {
                        childAdapter.setChildren(response.getChildren());
                        updateAttendanceCount();
                    } else {
                        childAdapter.setChildren(new ArrayList<>());
                        ViewUtils.showSnackbar(requireView(), getString(R.string.no_children));
                    }
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    ViewUtils.showSnackbar(requireView(), getString(R.string.error_loading_children));
                    Log.e(TAG, "Error loading children: " + error);
                });
            }
        });
    }

    private void loadEventsForCurrentDate() {
        loadEventsForDate(LocalDate.now());
    }

    private void loadEventsForDate(LocalDate date) {
        setLoading(true);
        repository.getEvents(date, 10, new DataCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> events) {
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    eventAdapter.setEvents(events != null ? events : new ArrayList<>());
                    // Clear selection when date changes
                    selectedEventId = null;
                    eventAdapter.setSelectedEventId(null);
                    childAdapter.setActivitySelected(false);
                    updateAttendanceCount();
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    ViewUtils.showSnackbar(requireView(), getString(R.string.error_loading_events));
                    Log.e(TAG, "Error loading events: " + error);
                });
            }
        });
    }

    private void loadEventAttendance(UUID eventId) {
        if (squadId == null || eventId == null) return;

        setLoading(true);
        repository.getEventAttendance(eventId, squadId, new DataCallback<GetAttendanceResponse>() {
            @Override
            public void onSuccess(GetAttendanceResponse response) {
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    if (response != null && response.getChildIds() != null) {
                        Set<UUID> presentIds = new HashSet<>(response.getChildIds());
                        childAdapter.setPresentChildIds(presentIds);
                        childAdapter.setActivitySelected(true);
                        updateAttendanceCount();
                    }
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    ViewUtils.showSnackbar(requireView(), getString(R.string.error_loading_attendance));
                    Log.e(TAG, "Error loading attendance: " + error);
                });
            }
        });
    }

    private void updateAttendanceCount() {
        int presentCount = childAdapter.getPresentCount();
        int totalCount = childAdapter.getItemCount();

        if (selectedEventId != null) {
            attendanceCountText.setText(getString(R.string.attendance_count_format, presentCount, totalCount));
        } else {
            attendanceCountText.setText(getString(R.string.attendance_not_selected));
        }
    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        childrenRecyclerView.setEnabled(!loading);
        eventsRecyclerView.setEnabled(!loading);
        datePicker.setEnabled(!loading);
        calendarIcon.setEnabled(!loading);
    }

    @Override
    public void onAttendanceChanged(Child child, boolean isPresent) {
        if (selectedEventId == null || child.getId() == null) return;

        // Freeze checkbox during request
        childAdapter.setActivitySelected(false);

        repository.updateAttendance(selectedEventId, child.getId(), isPresent, new DataCallback<com.sfedu.campus.generated.model.UpdateAttendanceResponse>() {
            @Override
            public void onSuccess(com.sfedu.campus.generated.model.UpdateAttendanceResponse response) {
                requireActivity().runOnUiThread(() -> {
                    childAdapter.setActivitySelected(true);
                    updateAttendanceCount();
                    String message = isPresent ?
                        getString(R.string.attendance_marked_present) :
                        getString(R.string.attendance_marked_absent);
                    ViewUtils.showSnackbar(requireView(), message);
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    childAdapter.setActivitySelected(true);
                    // Revert checkbox state on error
                    childAdapter.notifyDataSetChanged();
                    ViewUtils.showSnackbar(requireView(), getString(R.string.error_marking_attendance));
                    Log.e(TAG, "Error marking attendance: " + error);
                });
            }
        });
    }

    @Override
    public void onEventClick(Event event) {
        if (event.getId() == null) return;

        // Toggle selection
        if (selectedEventId != null && selectedEventId.equals(event.getId())) {
            // Deselect
            selectedEventId = null;
            eventAdapter.setSelectedEventId(null);
            childAdapter.setActivitySelected(false);
            childAdapter.setPresentChildIds(new HashSet<>());
        } else {
            // Select new event
            selectedEventId = event.getId();
            eventAdapter.setSelectedEventId(selectedEventId);
            loadEventAttendance(selectedEventId);
        }
        updateAttendanceCount();
    }
}