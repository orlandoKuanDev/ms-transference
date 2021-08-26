package com.example.mstransference.service;

import com.example.mstransference.model.Transference;
import com.example.mstransference.repositories.IRepository;
import com.example.mstransference.repositories.ITransferenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransferenceService extends BaseService<Transference, String> implements ITransferenceService{

    private final ITransferenceRepository repository;

    @Autowired
    public TransferenceService(ITransferenceRepository repository) {
        this.repository = repository;
    }

    @Override
    protected IRepository<Transference, String> getRepository() {
        return repository;
    }
}
