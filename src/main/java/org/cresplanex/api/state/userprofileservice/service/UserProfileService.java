package org.cresplanex.api.state.userprofileservice.service;

import lombok.extern.slf4j.Slf4j;
import org.cresplanex.api.state.common.service.BaseService;
import org.cresplanex.api.state.userprofileservice.entity.UserProfileEntity;
import org.cresplanex.api.state.userprofileservice.exception.NotFoundUserException;
import org.cresplanex.api.state.userprofileservice.exception.UserProfileNotFoundException;
import org.cresplanex.api.state.userprofileservice.repository.UserProfileRepository;
import org.cresplanex.api.state.userprofileservice.saga.model.userprofile.CreateUserProfileSaga;
import org.cresplanex.api.state.userprofileservice.saga.state.userprofile.CreateUserProfileSagaState;
import org.cresplanex.core.saga.orchestration.SagaInstanceFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserProfileService extends BaseService {

    private final UserProfileRepository userProfileRepository;
    private final SagaInstanceFactory sagaInstanceFactory;

    private final CreateUserProfileSaga createUserProfileSaga;

    @Transactional(readOnly = true)
    public UserProfileEntity findById(String userProfileId) {
        return internalFindById(userProfileId);
    }

    private UserProfileEntity internalFindById(String userProfileId) {
        return userProfileRepository.findById(userProfileId).orElseThrow(() -> new UserProfileNotFoundException(
                UserProfileNotFoundException.FindType.BY_ID,
                userProfileId
        ));
    }

    @Transactional(readOnly = true)
    public UserProfileEntity findByUserId(String userId) {
        return userProfileRepository.findByUserId(userId).orElseThrow(() -> new UserProfileNotFoundException(
                UserProfileNotFoundException.FindType.BY_USER_ID,
                userId
        ));
    }

    @Transactional(readOnly = true)
    public UserProfileEntity findByEmail(String email) {
        return userProfileRepository.findByEmail(email).orElseThrow(() -> new UserProfileNotFoundException(
                UserProfileNotFoundException.FindType.BY_EMAIL,
                email
        ));
    }

    @Transactional(readOnly = true)
    public List<UserProfileEntity> get() {
        return userProfileRepository.findAll();
    }

    // Messaging Handler内で処理されるため, Transactionalは親のTransactionに参加する
//    @Transactional
    public void beginCreate(UserProfileEntity profile) {
        CreateUserProfileSagaState.InitialData initialData = CreateUserProfileSagaState.InitialData.builder()
                .userId(profile.getUserId())
                .name(profile.getName())
                .email(profile.getEmail())
                .nickname(profile.getNickname())
                .build();
        CreateUserProfileSagaState state = new CreateUserProfileSagaState();
        state.setInitialData(initialData);

        String jobId = getJobId();
        state.setJobId(jobId);

        sagaInstanceFactory.create(createUserProfileSaga, state);
    }

    public UserProfileEntity create(UserProfileEntity profile) {
        return userProfileRepository.save(profile);
    }

    public void undoCreate(String userProfileId) {
        userProfileRepository.deleteById(userProfileId);
    }

    public void validateUsers(List<String> userIds) {
        List<String> existUserIds = userProfileRepository.findAllByUserIdIn(userIds)
                .stream()
                .map(UserProfileEntity::getUserId)
                .toList();
        if (existUserIds.size() != userIds.size()) {
            List<String> notExistUserIds = userIds.stream()
                    .filter(userId -> !existUserIds.contains(userId))
                    .toList();
            throw new NotFoundUserException(notExistUserIds);
        }
    }
}
