package com.example.theatre.service;

import com.example.theatre.dto.UserResponse;
import com.example.theatre.dto.UserUpdateRequest;
import com.example.theatre.entity.User;
import com.example.theatre.exception.BusinessRuleException;
import com.example.theatre.exception.ResourceNotFoundException;
import com.example.theatre.mapper.UserMapper;
import com.example.theatre.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository repository;
    public UserService(UserRepository repository) { this.repository = repository; }
    public Page<UserResponse> getAll(Pageable pageable) { return repository.findAll(pageable).map(UserMapper::toResponse); }
    public UserResponse getById(Long id) { return UserMapper.toResponse(findUser(id)); }
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = findUser(id);
        if (repository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) throw new BusinessRuleException("Email is already registered");
        UserMapper.update(user, request);
        return UserMapper.toResponse(repository.save(user));
    }
    @Transactional
    public void delete(Long id) { repository.delete(findUser(id)); }
    private User findUser(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id)); }
}
