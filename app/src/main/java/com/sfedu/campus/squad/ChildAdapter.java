package com.sfedu.campus.squad;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.sfedu.campus.R;
import com.sfedu.campus.generated.model.Child;
import com.sfedu.campus.generated.model.ChildTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChildAdapter extends RecyclerView.Adapter<ChildAdapter.ChildViewHolder> {

    public interface OnChildActionListener {
        void onEditNotes(Child child);
        void onCopyParentName(Child child);
        void onCopyParentPhone(Child child);
    }

    private final List<Child> children = new ArrayList<>();
    private final List<Child> filteredChildren = new ArrayList<>();
    private String currentQuery = "";
    private final OnChildActionListener listener;
    private final Context context;

    public ChildAdapter(Context context, OnChildActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_child_card, parent, false);
        return new ChildViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildViewHolder holder, int position) {
        Child child = filteredChildren.get(position);
        holder.bind(child);
    }

    @Override
    public int getItemCount() {
        return filteredChildren.size();
    }

    /**
     * Sets the main data source and refreshes the UI.
     */
    public void setChildren(List<Child> newChildren) {
        this.children.clear();
        if (newChildren != null) {
            this.children.addAll(newChildren);
        }
        Log.d("ChildAdapter", "setChildren: " + this.children.size() + " items");
        applyFilter(currentQuery);
    }

    /**
     * Updates the search query and refreshes the UI.
     */
    public void setFilter(String query) {
        this.currentQuery = query != null ? query : "";
        applyFilter(currentQuery);
    }

    private void applyFilter(String query) {
        List<Child> newFilteredList = new ArrayList<>();
        if (query.trim().isEmpty()) {
            newFilteredList.addAll(children);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (Child child : children) {
                if (matchesSearch(child, lowerQuery)) {
                    newFilteredList.add(child);
                }
            }
        }
        updateListWithDiff(newFilteredList);
    }

    /**
     * Uses DiffUtil to calculate changes and update the RecyclerView efficiently.
     */
    private void updateListWithDiff(List<Child> newList) {
        Log.i("ChildAdapter", "updateListWithDiff: " + newList.size() + " items");
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return filteredChildren.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return Objects.equals(filteredChildren.get(oldItemPosition).getId(), 
                                      newList.get(newItemPosition).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Child oldItem = filteredChildren.get(oldItemPosition);
                Child newItem = newList.get(newItemPosition);
                return Objects.equals(oldItem.getFullName(), newItem.getFullName()) &&
                       Objects.equals(oldItem.getAge(), newItem.getAge()) &&
                       Objects.equals(oldItem.getNotes(), newItem.getNotes()) &&
                       Objects.equals(oldItem.getParentFullName(), newItem.getParentFullName()) &&
                       Objects.equals(oldItem.getParentPhone(), newItem.getParentPhone()) &&
                       areTagsEqual(oldItem.getTags(), newItem.getTags());
            }
        });

        filteredChildren.clear();
        filteredChildren.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    private boolean areTagsEqual(List<ChildTag> tags1, List<ChildTag> tags2) {
        if (tags1 == tags2) return true;
        if (tags1 == null || tags2 == null) return false;
        if (tags1.size() != tags2.size()) return false;
        for (int i = 0; i < tags1.size(); i++) {
            if (!Objects.equals(tags1.get(i).getInfo(), tags2.get(i).getInfo())) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesSearch(Child child, String query) {
        if (child.getFullName() != null && child.getFullName().toLowerCase().contains(query)) {
            return true;
        }
        if (child.getParentFullName() != null && child.getParentFullName().toLowerCase().contains(query)) {
            return true;
        }
        if (child.getParentPhone() != null && child.getParentPhone().toLowerCase().contains(query)) {
            return true;
        }
        if (child.getNotes() != null && child.getNotes().toLowerCase().contains(query)) {
            return true;
        }
        if (child.getTags() != null) {
            for (ChildTag tag : child.getTags()) {
                if (tag.getInfo() != null && tag.getInfo().toLowerCase().contains(query)) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Child> getFilteredChildren() {
        return new ArrayList<>(filteredChildren);
    }

    class ChildViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvChildName;
        private final TextView tvChildAge;
        private final TextView tvParentName;
        private final TextView tvParentPhone;
        private final TextView tvNotes;
        private final ChipGroup cgTags;
        private final ImageView ivCopyParentName;
        private final ImageView ivCopyParentPhone;
        private final ImageView ivEditNotes;

        ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChildName = itemView.findViewById(R.id.tv_child_name);
            tvChildAge = itemView.findViewById(R.id.tv_child_age);
            tvParentName = itemView.findViewById(R.id.tv_parent_name);
            tvParentPhone = itemView.findViewById(R.id.tv_parent_phone);
            tvNotes = itemView.findViewById(R.id.tv_notes);
            cgTags = itemView.findViewById(R.id.cg_tags);
            ivCopyParentName = itemView.findViewById(R.id.iv_copy_parent_name);
            ivCopyParentPhone = itemView.findViewById(R.id.iv_copy_parent_phone);
            ivEditNotes = itemView.findViewById(R.id.iv_edit_notes);
        }

        void bind(Child child) {
            tvChildName.setText(child.getFullName() != null ? child.getFullName() : "Неизвестно");
            tvChildAge.setText(child.getAge() != null && !child.getAge().isEmpty() ? child.getAge() : "—");
            
            String pName = child.getParentFullName();
            tvParentName.setText(pName != null ? "Родитель: " + pName : "Родитель: —");
            
            tvParentPhone.setText(child.getParentPhone() != null ? child.getParentPhone() : "—");
            
            String notes = child.getNotes();
            tvNotes.setText(notes != null && !notes.isEmpty() ? notes : "Заметок нет");

            setupTags(child.getTags());

            ivCopyParentName.setOnClickListener(v -> {
                if (pName != null && !pName.isEmpty()) {
                    copyToClipboard(pName);
                    showToast("Имя родителя скопировано");
                    if (listener != null) listener.onCopyParentName(child);
                }
            });

            ivCopyParentPhone.setOnClickListener(v -> {
                String phone = child.getParentPhone();
                if (phone != null && !phone.isEmpty()) {
                    copyToClipboard(phone);
                    showToast("Телефон родителя скопирован");
                    if (listener != null) listener.onCopyParentPhone(child);
                }
            });

            ivEditNotes.setOnClickListener(v -> {
                if (listener != null) listener.onEditNotes(child);
            });
        }

        private void setupTags(List<ChildTag> tags) {
            cgTags.removeAllViews();
            if (tags != null && !tags.isEmpty()) {
                cgTags.setVisibility(View.VISIBLE);
                for (ChildTag tag : tags) {
                    if (tag.getInfo() != null && !tag.getInfo().isEmpty()) {
                        Chip chip = new Chip(cgTags.getContext());
                        chip.setText(tag.getInfo());
                        chip.setClickable(false);
                        chip.setCheckable(false);
                        chip.setChipCornerRadius(16f);
                        chip.setPadding(8, 4, 8, 4);
                        chip.setTextSize(12f);
                        cgTags.addView(chip);
                    }
                }
            } else {
                cgTags.setVisibility(View.GONE);
            }
        }

        private void copyToClipboard(String text) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("copied_text", text);
            clipboard.setPrimaryClip(clip);
        }

        private void showToast(String message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
}
