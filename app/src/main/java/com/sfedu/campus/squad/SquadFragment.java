package com.sfedu.campus.squad;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sfedu.campus.R;
import com.sfedu.campus.data.datasource.DataCallback;
import com.sfedu.campus.generated.model.Child;
import com.sfedu.campus.generated.model.GetUserSquad200Response;
import com.sfedu.campus.helpers.PreferencesHelper;
import com.sfedu.campus.helpers.ViewUtils;
import com.sfedu.campus.profile.DataEditWaitButton;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SquadFragment extends Fragment implements ChildAdapter.OnChildActionListener {

    private static final String TAG = "SquadFragment";

    // UI Elements
    private TextView tvSquadTitle;
    private TextView tvChildrenCount;
    private TextInputEditText etSearch;
    private TextInputLayout tilSearch;
    private TextView tvNoSquad;
    private TextView tvNoChildren;
    private RecyclerView rvChildren;
    private ProgressBar progressBar;
    private View viewOverlay;
    private View cvHeader;

    // Data
    private PreferencesHelper preferencesHelper;
    private UserRepository userRepository;
    private SquadRepository squadRepository;
    private ChildAdapter childAdapter;
    private UUID currentSquadId;
    private List<Child> allChildren = new ArrayList<>();

    public SquadFragment() {
        // Required empty public constructor
    }

    public static SquadFragment newInstance() {
        return new SquadFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferencesHelper = new PreferencesHelper(requireContext());
        userRepository = new UserRepository(requireContext());
        squadRepository = new SquadRepository(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_squad, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");
        initSquadSection();
    }
    public void initSquadSection() {
        initViews(requireView());
        setupRecyclerView();
        setupSearchListener();
        loadSquadData();
    }

    private void initViews(View view) {
        tvSquadTitle = view.findViewById(R.id.tv_squad_title);
        tvChildrenCount = view.findViewById(R.id.tv_children_count);
        etSearch = view.findViewById(R.id.et_search);
        tilSearch = view.findViewById(R.id.til_search);
        tvNoSquad = view.findViewById(R.id.tv_no_squad);
        tvNoChildren = view.findViewById(R.id.tv_no_children);
        rvChildren = view.findViewById(R.id.rv_children);
        progressBar = view.findViewById(R.id.progress_bar);
        viewOverlay = view.findViewById(R.id.view_overlay);
        cvHeader = view.findViewById(R.id.cv_header);
    }

    private void setupRecyclerView() {
        childAdapter = new ChildAdapter(requireContext(), this);
        rvChildren.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvChildren.setAdapter(childAdapter);
        // Removed setHasFixedSize(true) as it was preventing proper height calculation
        Log.d(TAG, "setupRecyclerView: Adapter set, initial item count: " + childAdapter.getItemCount());
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                childAdapter.setFilter(s.toString());
                updateChildrenCount();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadSquadData() {
        Log.d(TAG, "loadSquadData: Checking squad assignment");
        setLoading(true);
        userRepository.getUserSquad(new DataCallback<GetUserSquad200Response>() {
            @Override
            public void onSuccess(GetUserSquad200Response response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (response == null || response.getSquadId() == null) {
                        Log.d(TAG, "onSuccess: No squad assigned");
                        onNoSquadAssigned();
                    } else {
                        currentSquadId = response.getSquadId();
                        Log.d(TAG, "onSuccess: Squad assigned, ID: " + currentSquadId);
                        preferencesHelper.saveSquadId(currentSquadId);
                        loadChildren();
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Log.e(TAG, "onError checking squad: " + error);
                    setLoading(false);
                    if (preferencesHelper.hasSquadId()) {
                        currentSquadId = preferencesHelper.getSquadId();
                        loadChildren();
                    } else {
                        onNoSquadAssigned();
                        ViewUtils.toast(requireView(), requireContext(), "Ошибка: " + error);
                    }
                });
            }
        });
    }

    private void loadChildren() {
        if (preferencesHelper.hasChildrenCache()) {
            Log.d(TAG, "loadChildren: Loading from cache");
            loadChildrenFromCache();
            // Only fetch fresh data if we have cache - let fetchChildrenFromApi update UI
            // but don't show "no children" if we already have cached data
            fetchChildrenFromApi(true); // hasCache = true
        } else {
            Log.d(TAG, "loadChildren: No cache, fetching from API");
            fetchChildrenFromApi(false); // hasCache = false
        }
    }

    private void loadChildrenFromCache() {
        allChildren = preferencesHelper.getChildrenList();
        String cachedSquadTitle = preferencesHelper.getSquadTitle();

        requireActivity().runOnUiThread(() -> {
            if (cachedSquadTitle != null) {
                tvSquadTitle.setText("Мой отряд: " + cachedSquadTitle);
            }
            if (allChildren != null && !allChildren.isEmpty()) {
                Log.d(TAG, "loadChildrenFromCache: Rendering " + allChildren.size() + " items");
                Log.d(TAG, "loadChildrenFromCache: Setting adapter with " + allChildren.size() + " items");
                childAdapter.setChildren(allChildren);
                Log.d(TAG, "loadChildrenFromCache: Adapter item count after setChildren: " + childAdapter.getItemCount());
                updateChildrenCount();
                showChildrenList();
            } else {
                Log.d(TAG, "loadChildrenFromCache: allChildren is null or empty");
            }
        });
    }

    private void fetchChildrenFromApi(boolean hasCache) {
        if (currentSquadId == null) {
            setLoading(false);
            return;
        }

        squadRepository.getChildrenBySquad(currentSquadId, new DataCallback<List<Child>>() {
            @Override
            public void onSuccess(List<Child> children) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    if (children != null && !children.isEmpty()) {
                        Log.d(TAG, "fetchChildrenFromApi: Received " + children.size() + " children");
                        Log.d(TAG, "fetchChildrenFromApi: First child non-null: " + (children.get(0) != null));
                        allChildren = children;
                        preferencesHelper.saveChildrenList(allChildren);

                        // CRITICAL: Update adapter then visibility
                        Log.d(TAG, "fetchChildrenFromApi: About to call setChildren with " + children.size() + " items");
                        childAdapter.setChildren(allChildren);
                        Log.d(TAG, "fetchChildrenFromApi: Adapter item count after setChildren: " + childAdapter.getItemCount());
                        updateChildrenCount();
                        showChildrenList();

                        // Force a layout pass to ensure RecyclerView renders
                        rvChildren.post(() -> {
                           rvChildren.requestLayout();
                           Log.d(TAG, "RecyclerView requestLayout called. ItemCount: " + childAdapter.getItemCount());
                        });

                        fetchSquadTitle();
                    } else {
                        Log.d(TAG, "fetchChildrenFromApi: Empty children list received");
                        // Only show "no children" if we don't have cached data
                        if (!hasCache) {
                            showNoChildren();
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Log.e(TAG, "fetchChildrenFromApi Error: " + error);
                    setLoading(false);
                    if (allChildren.isEmpty()) {
                        showNoChildren();
                        ViewUtils.toast(requireView(), requireContext(), "Ошибка загрузки: " + error);
                    }
                });
            }
        });
    }

    private void onNoSquadAssigned() {
        setLoading(false);
        tvNoSquad.setVisibility(View.VISIBLE);
        tvNoChildren.setVisibility(View.GONE);
        rvChildren.setVisibility(View.GONE);
        tilSearch.setVisibility(View.GONE);
        tvChildrenCount.setText("Детей: 0");
        tvSquadTitle.setText("Мой отряд");
    }

    private void showNoChildren() {
        tvNoSquad.setVisibility(View.GONE);
        tvNoChildren.setVisibility(View.VISIBLE);
        rvChildren.setVisibility(View.GONE);
        tilSearch.setVisibility(View.VISIBLE);
        tvChildrenCount.setText("Детей: 0");
    }

    private void showChildrenList() {
        Log.d(TAG, "showChildrenList: Making RecyclerView VISIBLE, adapter count: " + childAdapter.getItemCount());
        Log.d(TAG, "showChildrenList: cv_header height: " + cvHeader.getHeight() + ", width: " + cvHeader.getWidth());
        Log.d(TAG, "showChildrenList: parent height: " + requireView().getHeight() + ", width: " + requireView().getWidth());
        tvNoSquad.setVisibility(View.GONE);
        tvNoChildren.setVisibility(View.GONE);
        rvChildren.setVisibility(View.VISIBLE);
        tilSearch.setVisibility(View.VISIBLE);

        // Force layout
        rvChildren.post(() -> {
            rvChildren.requestLayout();
            Log.d(TAG, "showChildrenList: Forced layout, height: " + rvChildren.getHeight() + ", width: " + rvChildren.getWidth());
            Log.d(TAG, "showChildrenList: After requestLayout - cv_header height: " + cvHeader.getHeight() + ", parent height: " + requireView().getHeight());
        });
    }

    private void fetchSquadTitle() {
        if (currentSquadId == null) return;
        new Thread(() -> {
            try {
                com.sfedu.campus.generated.api.SquadsApi api = new com.sfedu.campus.generated.api.SquadsApi(
                        com.sfedu.campus.helpers.ApiProvider.getApiClient(requireContext())
                );
                com.sfedu.campus.generated.model.GetSquadChildrenResponse response = api.getChildrenBySquad(currentSquadId);
                if (response != null && response.getSquadTitle() != null && isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        String title = response.getSquadTitle();
                        preferencesHelper.saveSquadTitle(title);
                        tvSquadTitle.setText("Мой отряд: " + title);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "fetchSquadTitle error: " + e.getMessage());
            }
        }).start();
    }

    private void updateChildrenCount() {
        int count = childAdapter.getItemCount();
        tvChildrenCount.setText("Детей: " + count);
    }

    private void setLoading(boolean loading) {
        if (!isAdded()) return;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        viewOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onEditNotes(Child child) {
        showEditNotesDialog(child);
    }

    @Override
    public void onCopyParentName(Child child) {}

    @Override
    public void onCopyParentPhone(Child child) {}

    private void showEditNotesDialog(Child child) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_notes, null);
        TextInputEditText etNotes = dialogView.findViewById(R.id.et_notes);
        DataEditWaitButton btnSaveNotes = dialogView.findViewById(R.id.btn_save_notes);

        if (child.getNotes() != null) {
            etNotes.setText(child.getNotes());
        }

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Отмена", null)
                .create();
        dialog.show();

        // Enable/disable save button based on text changes
        etNotes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Enable button if there's text, disable if empty
                String currentText = s.toString().trim();
                String initialNotes = child.getNotes() != null ? child.getNotes().trim() : "";
                boolean isEnabled = !initialNotes.equals(currentText);
                Log.i(TAG, "onTextChanged: isEnabled=" + isEnabled + ", initialNotes=" + initialNotes + ", currentText=" + currentText + isEnabled);
                btnSaveNotes.setEnabled(isEnabled);
                // Also update the visual state if DataEditWaitButton has custom state handling
                if (isEnabled) {
                    btnSaveNotes.setActivated(true);
                } else {
                    btnSaveNotes.setActivated(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Set initial state based on current notes
        String initialNotes = child.getNotes() != null ? child.getNotes().trim() : "";
        btnSaveNotes.setEnabled(!initialNotes.equals(etNotes.getText().toString().trim()));
        if (!initialNotes.isEmpty()) {
            btnSaveNotes.setActivated(true);
        } else {
            btnSaveNotes.setActivated(false);
        }

        btnSaveNotes.setOnClickListener(v -> {
            String newNotes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";
            btnSaveNotes.showLoading();

            Log.i(TAG, "new Notes: " + newNotes);
            squadRepository.updateChildNotes(child.getId(), newNotes, new DataCallback<Child>() {
                @Override
                public void onSuccess(Child updatedChild) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        btnSaveNotes.hideLoading(false);
                        dialog.dismiss();
                        
                        // Update local data efficiently
                        for (int i = 0; i < allChildren.size(); i++) {
                            if (allChildren.get(i).getId().equals(updatedChild.getId())) {
                                allChildren.set(i, updatedChild);
                                break;
                            }
                        }
                        childAdapter.setChildren(new ArrayList<>(allChildren));
                        preferencesHelper.saveChildrenList(allChildren);
                        ViewUtils.toast(requireView(), requireContext(), "Заметки сохранены");
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        btnSaveNotes.hideLoading(true);
                        Log.e(TAG, "onError: " + error);
                        ViewUtils.toast(requireView(), requireContext(), "Ошибка: " + error);
                    });
                }
            });
        });
    }
}
