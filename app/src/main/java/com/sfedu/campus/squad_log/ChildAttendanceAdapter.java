package com.sfedu.campus.squad_log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sfedu.campus.R;
import com.sfedu.campus.generated.model.Child;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ChildAttendanceAdapter extends RecyclerView.Adapter<ChildAttendanceAdapter.ChildViewHolder> {

    private List<Child> children = new ArrayList<>();
    private Set<UUID> presentChildIds = new HashSet<>();
    private OnAttendanceChangeListener listener;
    private boolean isActivitySelected = false;

    public interface OnAttendanceChangeListener {
        void onAttendanceChanged(Child child, boolean isPresent);
    }

    public void setOnAttendanceChangeListener(OnAttendanceChangeListener listener) {
        this.listener = listener;
    }

    public void setChildren(List<Child> children) {
        this.children = children != null ? children : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setPresentChildIds(Set<UUID> presentChildIds) {
        this.presentChildIds = presentChildIds != null ? presentChildIds : new HashSet<>();
        notifyDataSetChanged();
    }

    public void setActivitySelected(boolean selected) {
        this.isActivitySelected = selected;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_child_attendance, parent, false);
        return new ChildViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildViewHolder holder, int position) {
        Child child = children.get(position);
        holder.bind(child);
    }

    @Override
    public int getItemCount() {
        return children.size();
    }

    public int getPresentCount() {
        return presentChildIds.size();
    }

    class ChildViewHolder extends RecyclerView.ViewHolder {
        private final TextView childName;
        private final CheckBox attendanceCheckbox;

        ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            childName = itemView.findViewById(R.id.child_name);
            attendanceCheckbox = itemView.findViewById(R.id.attendance_checkbox);
        }

        void bind(Child child) {
            childName.setText(child.getFullName() != null ? child.getFullName() : "");

            UUID childId = child.getId();
            boolean isPresent = childId != null && presentChildIds.contains(childId);

            // Set checkbox state
            attendanceCheckbox.setChecked(isPresent);

            // Enable/disable based on whether an activity is selected
            attendanceCheckbox.setEnabled(isActivitySelected);
            attendanceCheckbox.setAlpha(isActivitySelected ? 1.0f : 0.5f);

            // Set click listener
            attendanceCheckbox.setOnClickListener(v -> {
                if (!isActivitySelected || listener == null || childId == null) return;

                boolean newIsPresent = attendanceCheckbox.isChecked();

                // Update local state immediately for UI responsiveness
                if (newIsPresent) {
                    presentChildIds.add(childId);
                } else {
                    presentChildIds.remove(childId);
                }

                listener.onAttendanceChanged(child, newIsPresent);
            });
        }
    }
}