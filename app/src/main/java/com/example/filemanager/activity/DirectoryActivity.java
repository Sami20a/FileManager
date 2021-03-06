package com.example.filemanager.activity;

import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.SearchView;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.App;
import com.example.filemanager.R;
import com.example.filemanager.adapter.DirectoryItemsAdapter;
import com.example.filemanager.databinding.ActivityDirectoryBinding;
import com.example.filemanager.dialog.CopyDialog;
import com.example.filemanager.dialog.CreateDirectoryDialogFragment;
import com.example.filemanager.dialog.DeleteDirectoryItemDialogFragment;
import com.example.filemanager.dialog.DirectoryItemInfoDialogFragment;
import com.example.filemanager.dialog.ErrorDialogFragment;
import com.example.filemanager.dialog.RenameDirectoryItemDialogFragment;
import com.example.filemanager.dialog.SortTypeDialogFragment;
import com.example.filemanager.model.DirectoryItem;
import com.example.filemanager.repository.directory.DirectoryRepository;
import com.example.filemanager.repository.settings.SettingsRepository;
import com.example.filemanager.util.MimeTypeUtil;
import com.example.filemanager.viewmodel.DirectoryViewModel;

import java.util.List;

import io.reactivex.disposables.CompositeDisposable;

public class DirectoryActivity extends AppCompatActivity implements
        DirectoryItemsAdapter.Listener,
        SortTypeDialogFragment.Listener,
        CreateDirectoryDialogFragment.Listener,
        RenameDirectoryItemDialogFragment.Listener,
        DeleteDirectoryItemDialogFragment.Listener
{
    private static final String DIRECTORY_INTENT_KEY = "DIRECTORY_INTENT_KEY";
    private static final String SEARCH_VIEW_QUERY_KEY = "SEARCH_VIEW_QUERY_KEY";

    private CompositeDisposable viewModelDisposable = new CompositeDisposable();
    private DirectoryViewModel viewModel;

    private ActivityDirectoryBinding binding;

    private DirectoryItemsAdapter adapter = new DirectoryItemsAdapter(this);
    private LinearLayoutManager recyclerViewLayoutManager = new LinearLayoutManager(this);

    private CopyDialog copyDialog = new CopyDialog();

    private SearchView searchView;
    private String searchQuery = "";

    private ActionMode.Callback actionModeCallback = new ActionModeCallback();
    private ActionMode actionMode;


    public static void start(@NonNull Context context, @NonNull String directory) {
        Intent intent = new Intent(context, DirectoryActivity.class);
        intent.putExtra(DIRECTORY_INTENT_KEY, directory);
        context.startActivity(intent);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_directory);

        initActionBar();
        initDirectoryItemsRecyclerView();
        initPasteButton();

        createViewModel();
    }

    @Override
    protected void onStart() {
        super.onStart();
        subscribeToViewModel();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unsubscribeFromViewModel();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        String searchQuery = searchView.getQuery().toString();
        outState.putString(SEARCH_VIEW_QUERY_KEY, searchQuery);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            searchQuery = savedInstanceState.getString(SEARCH_VIEW_QUERY_KEY, "");
        }
    }

    @Override
    public void onBackPressed() {
        if (closeSearchViewIfOpened()) {
            return;
        }
        viewModel.handleBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_directory_activity, menu);

        MenuItem searchItem = menu.findItem(R.id.item_search);
        searchView = (SearchView) searchItem.getActionView();
        initSearchView();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                if (closeSearchViewIfOpened()) {
                    return true;
                }
                viewModel.handleActionBarBackPressed();
                break;
            }
            case R.id.item_sort: {
                viewModel.handleChangeSortTypeClicked();
                break;
            }
            case R.id.item_create_directory: {
                viewModel.handleCreateDirectoryClicked();
                break;
            }
            case R.id.item_show_or_hide_hidden_files: {
                viewModel.handleShowOrHideHiddenFilesClicked();
                break;
            }
            default: {
                break;
            }
        }
        return true;
    }


    @Override
    public void onDirectoryItemClicked(@NonNull DirectoryItem item) {
       viewModel.handleItemClicked(item);
    }

    @Override
    public void onDirectoryItemCutClicked(@NonNull DirectoryItem item) {
        viewModel.handleCutClicked(item);
    }

    @Override
    public void onDirectoryItemCopyClicked(@NonNull DirectoryItem item) {
        viewModel.handleCopyClicked(item);
    }

    @Override
    public void onDirectoryItemRenameClicked(@NonNull DirectoryItem item) {
        viewModel.handleRenameClicked(item);
    }

    @Override
    public void onDirectoryItemDeleteClicked(@NonNull DirectoryItem item) {
        viewModel.handleDeleteClicked(item);
    }

    @Override
    public void onDirectoryItemInfoClicked(@NonNull DirectoryItem item) {
        viewModel.handleInfoClicked(item);
    }

    @Override
    public void onDirectoryItemShareClicked(@NonNull DirectoryItem item) {
        viewModel.handleShareClicked(item);
    }

    @Override
    public void onItemSelectionChanged(boolean isInSelectMode) {
        showActionModeVisible(isInSelectMode);
    }


    @Override
    public void onSortTypeChanged() {
        viewModel.handleSortTypeChanged();
    }

    @Override
    public void onCreateDirectory(@NonNull String directoryName) {
        viewModel.handleCreateDirectoryConfirmed(directoryName);
    }

    @Override
    public void onRenameDirectoryItem(@NonNull String newName, @NonNull DirectoryItem item) {
        viewModel.handleRenameConfirmed(newName, item);
    }

    @Override
    public void onDeleteDirectoryItem(@NonNull DirectoryItem item) {
        viewModel.handleDeleteConfirmed(item);
    }

    @Override
    public void onDeleteDirectoryItems() {
        List<DirectoryItem> selectedItems = adapter.getSelectedItems();
        showActionModeVisible(false);
        viewModel.handleDeleteConfirmed(selectedItems);
    }


    private void initActionBar() {
        setSupportActionBar(binding.toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            return;
        }

        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
    }

    private void initDirectoryItemsRecyclerView() {
        binding.directoryContentRecyclerView.setLayoutManager(recyclerViewLayoutManager);
        binding.directoryContentRecyclerView.setAdapter(adapter);
    }

    private void initSearchView() {
        restoreSearchView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(@NonNull String query) {
                viewModel.handleSearchQueryChanged(query);
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(@NonNull String query) {
                return false;
            }
        });
    }

    private void initPasteButton() {
        binding.pasteFab.setOnClickListener(v -> viewModel.handlePasteClicked());
    }


    private void createViewModel() {
        String directory = getIntent().getStringExtra(DIRECTORY_INTENT_KEY);

        App app = (App) getApplication();
        DirectoryRepository directoryRepository = app.getDirectoryRepository();
        SettingsRepository settingsRepository = app.getSettingsRepository();

        ViewModelProvider.Factory viewModelFactory = new DirectoryViewModel.Factory(
                directory,
                directoryRepository,
                settingsRepository
        );

        viewModel = ViewModelProviders
                .of(this, viewModelFactory)
                .get(DirectoryViewModel.class);
    }

    private void subscribeToViewModel() {
        viewModelDisposable.addAll(
                viewModel.isLoading.subscribe(this::showIsLoading),
                viewModel.currentDirectory.subscribe(this::showCurrentDirectory),
                viewModel.directoryContent.subscribe(this::showDirectoryContent),
                viewModel.error.subscribe(this::showError),
                viewModel.searchQuery.subscribe(this::showSearchQuery),
                viewModel.isCopyModeEnabled.subscribe(this::showCopyModeEnabled),
                viewModel.isCopyDialogVisible.subscribe(this::showOrHideCopyDialog),

                viewModel.showSortTypeDialogEvent.subscribe(u -> showSortTypeDialog()),
                viewModel.showCreateDirectoryDialogEvent.subscribe(u -> showCreateDirectoryDialog()),
                viewModel.showRenameItemDialogEvent.subscribe(this::showRenameDirectoryItemDialog),
                viewModel.showDeleteItemDialogEvent.subscribe(this::showDeleteDirectoryItemDialog),
                viewModel.showDeleteItemsDialogEvent.subscribe(this::showDeleteDirectoryItemsDialog),
                viewModel.showInfoItemDialogEvent.subscribe(this::showDirectoryItemInfoDialog),
                viewModel.openFileEvent.subscribe(this::openFile),
                viewModel.shareFileEvent.subscribe(this::shareFile),
                viewModel.closeScreenEvent.subscribe(u -> closeScreen())
        );
    }

    private void unsubscribeFromViewModel() {
        viewModelDisposable.clear();
    }


    private void showIsLoading(boolean isLoading) {
        binding.directoryContentRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        binding.directoryContentLoadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showCurrentDirectory(@NonNull String currentDirectory) {
        binding.directoryPathTextView.setText(currentDirectory);
    }

    private void showDirectoryContent(@NonNull List<DirectoryItem> directoryContent) {
        boolean isEmpty = directoryContent.isEmpty();
        binding.emptyDirectoryTextView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

        adapter.setData(directoryContent);
        recyclerViewLayoutManager.scrollToPositionWithOffset(0, 0);
    }

    private void showError(@NonNull Throwable error) {
        ErrorDialogFragment dialog = ErrorDialogFragment.newInstance(error);
        dialog.show(getSupportFragmentManager(), "ErrorDialogFragment");
    }

    private void showSearchQuery(@NonNull String searchQuery) {
        adapter.setSearchQuery(searchQuery);
    }

    @SuppressLint("RestrictedApi")
    private void showCopyModeEnabled(boolean isCopyModeEnabled) {
        int fabVisibility = isCopyModeEnabled ? View.VISIBLE : View.GONE;
        int iconId = isCopyModeEnabled ? R.drawable.ic_clear : R.drawable.ic_arrow_back;

        binding.pasteFab.setVisibility(fabVisibility);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(iconId);
        }
    }

    private void showOrHideCopyDialog(boolean show) {
        if (show) {
            copyDialog.show(this);
        } else {
            copyDialog.dismiss();
        }
    }

    private void showSortTypeDialog() {
        SortTypeDialogFragment dialog = new SortTypeDialogFragment();
        dialog.show(getSupportFragmentManager(), "SortTypeDialogFragment");
    }

    private void showRenameDirectoryItemDialog(@NonNull DirectoryItem item) {
        RenameDirectoryItemDialogFragment dialog = RenameDirectoryItemDialogFragment.newInstance(item);
        dialog.show(getSupportFragmentManager(), "RenameDirectoryItemDialogFragment");
    }

    private void showDeleteDirectoryItemDialog(@NonNull DirectoryItem item) {
        DeleteDirectoryItemDialogFragment dialog = DeleteDirectoryItemDialogFragment.newInstance(item);
        dialog.show(getSupportFragmentManager(), "DeleteDirectoryItemDialogFragment");
    }

    private void showDeleteDirectoryItemsDialog(@NonNull List<DirectoryItem> items) {
        DeleteDirectoryItemDialogFragment dialog = DeleteDirectoryItemDialogFragment.newInstance(items);
        dialog.show(getSupportFragmentManager(), "DeleteDirectoryItemDialogFragment");
    }

    private void showDirectoryItemInfoDialog(@NonNull DirectoryItem item) {
        DirectoryItemInfoDialogFragment dialog = DirectoryItemInfoDialogFragment.newInstance(item);
        dialog.show(getSupportFragmentManager(), "DirectoryItemInfoDialogFragment");
    }

    private void showCreateDirectoryDialog() {
        CreateDirectoryDialogFragment dialog = CreateDirectoryDialogFragment.newInstance();
        dialog.show(getSupportFragmentManager(), "CreateDirectoryDialogFragment");
    }

    private void showActionModeVisible(boolean isActionModeVisible) {
        if (isActionModeVisible && actionMode == null) {
            actionMode = startActionMode(actionModeCallback);
            return;
        }

        if (!isActionModeVisible && actionMode != null) {
            actionMode.finish();
            actionMode = null;
        }
    }

    private void openFile(@NonNull DirectoryItem item) {
        startActivity(item, Intent.ACTION_VIEW);
    }

    private void shareFile(@NonNull DirectoryItem item) {
        startActivity(item, Intent.ACTION_SEND);
    }

    private void startActivity(@NonNull DirectoryItem item, @NonNull String action) {
        Uri uri = Uri.parse(item.getUri());
        String mimeType = MimeTypeUtil.getMimeType(item);

        Intent intent = new Intent();
        intent.setAction(action);
        intent.setDataAndType(uri, mimeType);

        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, R.string.unable_to_open_file, Toast.LENGTH_LONG).show();
        } else {
            startActivity(intent);
        }
    }

    private void closeScreen() {
        finish();
    }


    private boolean closeSearchViewIfOpened() {
        if (!searchView.isIconified()) {
            searchView.setIconified(true);
            return true;
        }
        return false;
    }

    private void restoreSearchView() {
        if (searchQuery.isEmpty()) {
            return;
        }

        searchView.setIconified(false);
        searchView.setQuery(searchQuery, false);
    }


    private class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            actionMode.getMenuInflater().inflate(R.menu.menu_directory_activity_action_mode, menu);
            actionMode.setTitle(R.string.select_items);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            List<DirectoryItem> selectedItems = adapter.getSelectedItems();

            switch (menuItem.getItemId()) {
                case R.id.item_action_mode_delete: {
                    viewModel.handleDeleteClicked(selectedItems);
                    return true;
                }
                case R.id.item_action_mode_cut: {
                    viewModel.handleCutClicked(selectedItems);
                    break;
                }
                case R.id.item_action_mode_copy: {
                    viewModel.handleCopyClicked(selectedItems);
                    break;
                }
                case R.id.item_action_mode_select_all: {
                    adapter.selectAll();
                    return true;
                }
            }

            showActionModeVisible(false);

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            DirectoryActivity.this.actionMode = null;
            DirectoryActivity.this.adapter.resetSelection();
        }
    }
}
