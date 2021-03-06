package com.example.filemanager.repository.storage;

import android.support.annotation.NonNull;

import com.example.filemanager.model.StorageModel;
import com.example.filemanager.model.exception.LoadStoragesException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class MockStorageRepository implements StorageRepository {
    private List<StorageModel> storageModelList = new ArrayList<>();

    public MockStorageRepository() {
        storageModelList.add(new StorageModel("Internal storage", 9, 7));
        storageModelList.add(new StorageModel("External storage", 19, 2));
        storageModelList.add(new StorageModel("External storage", 19, 2));
        storageModelList.add(new StorageModel("External storage", 19, 2));
        storageModelList.add(new StorageModel("External storage", 19, 2));
        storageModelList.add(new StorageModel("External storage", 19, 2));
        storageModelList.add(new StorageModel("External storage", 19, 2));
        storageModelList.add(new StorageModel("External storage", 19, 2));
        storageModelList.add(new StorageModel("External storage", 19, 2));
    }

    @Override
    @NonNull
    public Single<List<StorageModel>> getStorageList() {
        //return Single.error(new LoadStoragesException(new Exception()));
        return Single.just(storageModelList)
                .delay(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io());
    }
}
