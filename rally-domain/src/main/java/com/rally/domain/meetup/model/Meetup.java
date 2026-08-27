package com.rally.domain.meetup.model;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.court.model.CourtData;
import com.rally.domain.meetup.convert.MeetupDomainConvertMapper;
import com.rally.domain.meetup.enums.*;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.user.enums.GenderEnum;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.utils.Assert;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 约球聚合根（包含活动信息 + 所有参与者报名信息）
 * <p>
 * 所有针对约球和报名的领域操作都应通过此聚合根进行。
 */
@Getter
public class Meetup {

    private final MeetupData data;

    /** 所有参与者的报名记录（创建者、等待审批、已通过等） */
    private final List<RegistrationData> registrations;

    /** 加载时身份快照，仅用于 I1 的不可变校验，不参与持久化。 */
    private final String loadedMeetupId;
    private final Map<RegistrationData, String> loadedRegistrationIds = new IdentityHashMap<>();
    private final Map<RegistrationData, String> loadedRegistrationMeetupIds = new IdentityHashMap<>();

    /**
     * 完整聚合根（约球数据 + 报名记录）
     */
    public Meetup(MeetupData data, List<RegistrationData> registrations) {
        this.data = data;
        this.registrations = registrations != null ? registrations : new ArrayList<>();
        this.loadedMeetupId = data != null ? data.getBizId() : null;
        this.registrations.forEach(registration -> {
            if (registration != null) {
                loadedRegistrationIds.put(registration, registration.getBizId());
                loadedRegistrationMeetupIds.put(registration, registration.getRallyMeetupId());
            }
        });
    }

    public String getMeetupId() {
        return this.data.getBizId();
    }

    // ======================== 约球状态判断 ========================

    /**
     * 懒判定：计算真实状态
     * - OPEN + endTime 已过 → FINISHED
     * - OPEN + startTime 已过 + endTime 未过 → ONGOING
     */
    public MeetupStatusEnum getRealStatus() {
        return getRealStatus(LocalDateTime.now());
    }

    /**
     * 使用调用方冻结的当前时间解释存储状态。仅 OPEN 产生读取态，且边界时刻
     * （恰到开始/结束）分别视为 ONGOING/FINISHED。
     */
    public MeetupStatusEnum getRealStatus(LocalDateTime now) {
        MeetupStatusEnum status = data.getStatus();
        if (status == MeetupStatusEnum.OPEN) {
            if (!now.isBefore(data.getEndTime())) {
                return MeetupStatusEnum.FINISHED;
            }
            if (!now.isBefore(data.getStartTime())) {
                return MeetupStatusEnum.ONGOING;
            }
        }
        return status;
    }

    /** 是否已过期（开始时间已过） */
    public boolean isExpired() {
        return isExpired(LocalDateTime.now());
    }

    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(data.getStartTime());
    }

    /** 开始时间已过的约球不能再接受赛约确认。 */
    public void assertNotExpired() {
        assertNotExpired(LocalDateTime.now());
    }

    public void assertNotExpired(LocalDateTime now) {
        if (isExpired(now)) {
            throw new BusinessException(BizErrorCode.MEETUP_EXPIRED);
        }
    }

    /** 是否为活跃状态（非终态） */
    public boolean isActive() {
        return isActive(LocalDateTime.now());
    }

    public boolean isActive(LocalDateTime now) {
        MeetupStatusEnum realStatus = getRealStatus(now);
        return realStatus != MeetupStatusEnum.FINISHED && realStatus != MeetupStatusEnum.CLOSED;
    }

    /** 是否已满 */
    public boolean isFull() {
        return countApprovedPlayers() >= data.getMaxPlayers();
    }

    /**
     * 统计已批准的参与者数量（含创建者）
     */
    public int countApprovedPlayers() {
        return (int) registrations.stream()
                .filter(RegistrationData::isActiveParticipant)
                .count();
    }

    /** 保存聚合前统一由有效报名重算人数，创建者不额外计数。 */
    public void recalculateCurrentPlayers() {
        data.setCurrentPlayers(countApprovedPlayers());
    }

    /** 获取创建人 userId */
    public String getCreatorId() {
        return data.getCreatorId();
    }

    /** 是否为创建人 */
    public boolean isCreator(String userId) {
        return userId.equals(data.getCreatorId());
    }

    /**
     * 是否为有效参与者（创建人或 JOINED/REVIEWED/SKIPPED）。
     * PENDING 只属于活跃报名，不计入参与者和活动人数；
     * 其聊天、比分与逐项评价入口资格由 {@link #assertIn(String)} 单独判定。
     */
    public boolean isParticipant(String userId) {
        if (isCreator(userId)) {
            return true;
        }
        RegistrationData registration = findActiveRegistration(userId);
        return registration != null && registration.isActiveParticipant();
    }

    /** 是否应给该用户发送通知（仍是活动成员：创建人或有效参与者；已退出/被拒则不再通知） */
    public boolean shouldNotice(String userId) {
        return isParticipant(userId);
    }

    /** 是否存在创建人以外的有效参与者 */
    public boolean hasOtherParticipants() {
        return registrations.stream().filter(RegistrationData::isActiveParticipant).anyMatch(r -> !isCreator(r.getUserId()));
    }

    /** 断言当前用户为创建人，否则抛出异常 */
    public void assertOwner(String userId) {
        if (!isCreator(userId)) {
            throw new BusinessException(BizErrorCode.NOT_CREATOR);
        }
    }

    /**
     * 断言用户可进入活动协作入口：创建者或持有活跃报名的用户。
     * 活跃报名包含 PENDING，因此待审批用户可聊天，也可在
     * ONGOING/FINISHED 且未过期时经 {@link #assertReviewAvailable(String)}
     * 提交比分或逐项评价。这不改变其不占名额的语义。
     *
     * @param userId 用户ID
     */
    public void assertIn(String userId) {
        // 创建者有权限
        if (isCreator(userId)) {
            return;
        }
        // PENDING/JOINED/REVIEWED/SKIPPED 活跃报名均可进入协作入口
        RegistrationData registration = findActiveRegistration(userId);
        Assert.notNull(registration, BizErrorCode.NOT_JOINED);
    }

    /** 是否可编辑 */
    public boolean canEdit(String userId, int lockMinutes) {
        MeetupStatusEnum realStatus = getRealStatus();
        return isCreator(userId)
                && realStatus != MeetupStatusEnum.FINISHED
                && realStatus != MeetupStatusEnum.CLOSED
                && LocalDateTime.now().isBefore(data.getStartTime().minusMinutes(lockMinutes));
    }

    /**
     * 判断场地是否变更
     * @param cmd 编辑命令
     * @return true 表示场地变更
     */
    public boolean isLocationChanged(MeetupPublishCmd cmd) {
        // 场地名称变更
        if (cmd.getCourtName() != null && !cmd.getCourtName().equals(data.getCourtName())) {
            return true;
        }
        // 场地地址变更
        if (cmd.getCourtAddress() != null && !cmd.getCourtAddress().equals(data.getCourtAddress())) {
            return true;
        }
        // 经纬度变更
        if (cmd.getCourtLng() != null && cmd.getCourtLat() != null) {
            return !cmd.getCourtLng().equals(data.getCourtLng()) || !cmd.getCourtLat().equals(data.getCourtLat());
        }
        return false;
    }

    /**
     * C3 兼容入口：应用层已按历史顺序完成赛事、状态、城市和参与者锁定校验，
     * 聚合在映射前最后校验操作人，映射后不追加发布级校验或人数重算。
     */
    public void edit(String userId, MeetupPublishCmd cmd, CourtData courtData) {
        assertOwner(userId);
        MeetupDomainConvertMapper.INSTANCE.updateMeetupData(data, cmd, courtData);
        // 兼容提示要求保留命中球场库时多源映射可能覆盖身份/审计字段的历史缺陷，
        // 因而这里不追加会把该缺陷转化成新业务拒绝的身份断言。
    }

    /**
     * C3 完整领域入口。校验顺序刻意保留既有契约：赛事许可、状态/锁定点、
     * 城市与参与者锁定均先于创建者权限；不重跑普通发布的 duration/NTRP 组合校验。
     */
    public void edit(String userId, MeetupPublishCmd cmd, CourtData courtData,
                     boolean tournamentEditAllowed, int lockMinutes, LocalDateTime now) {
        if (MeetupTypeEnum.TOURNAMENT.getCode().equals(data.getMeetupType()) && !tournamentEditAllowed) {
            throw new BusinessException(BizErrorCode.MEETUP_TOURNAMENT_EDIT_FORBIDDEN);
        }
        MeetupStatusEnum realStatus = getRealStatus(now);
        if (realStatus == MeetupStatusEnum.CLOSED
                || realStatus == MeetupStatusEnum.FINISHED
                || realStatus == MeetupStatusEnum.ONGOING
                || !now.isBefore(data.getStartTime().minusMinutes(lockMinutes))) {
            throw new BusinessException(BizErrorCode.MEETUP_STATUS_ILLEGAL);
        }
        if (cmd.getCityCode() != null && !cmd.getCityCode().equals(data.getCityCode())) {
            throw new BusinessException(BizErrorCode.CITY_CHANGE_FORBIDDEN);
        }
        if (countApprovedPlayers() > 1) {
            boolean timeChanged = cmd.getStartTime() != null && !cmd.getStartTime().equals(data.getStartTime());
            boolean durationChanged = cmd.getDuration() != null && cmd.getDuration().compareTo(data.getDuration()) != 0;
            if (timeChanged || durationChanged || isLocationChanged(cmd)) {
                throw new BusinessException(BizErrorCode.LOCATION_TIME_CHANGE_FORBIDDEN);
            }
        }
        edit(userId, cmd, courtData);
    }

    // ======================== 报名记录查询 ========================

    /** 查找用户的活跃报名记录（PENDING/JOINED/REVIEWED/SKIPPED）。 */
    public RegistrationData findActiveRegistration(String userId) {
        return registrations.stream()
                .filter(r -> userId.equals(r.getUserId()))
                .filter(r -> r.isPending() || r.isActiveParticipant())
                .findFirst().orElse(null);
    }



    // ======================== 报名领域行为 ========================

    /**
     * 报名（包含校验 + 创建报名记录）
     *
     * @param userProfile    用户档案领域对象
     * @param autoWithdrawAt 自动撤回时间，可为 null
     */
    public RegistrationStatusEnum join(UserProfile userProfile, LocalDateTime autoWithdrawAt) {
        return join(userProfile, autoWithdrawAt, LocalDateTime.now());
    }

    /** C4：使用一次调用冻结的当前时间申请或直接加入。 */
    public RegistrationStatusEnum join(UserProfile userProfile, LocalDateTime autoWithdrawAt, LocalDateTime now) {
        // 1. 校验
        assertCanJoin(userProfile, now);

        // 2. 创建报名记录
        RegistrationData registration = new RegistrationData();
        registration.setRallyMeetupId(data.getBizId());
        registration.setUserId(userProfile.getUser().getUserId());
        registration.setExpiresAt(autoWithdrawAt);

        // 3. 根据加入模式设置状态
        registration.setStatus(data.getJoinMode() == JoinModeEnum.DIRECT ? RegistrationStatusEnum.JOINED : RegistrationStatusEnum.PENDING);
        this.registrations.add(registration);

        validateAfterCommand();
        return registration.getStatus();

    }

    /**
     * 断言可以报名（校验约球状态、时间、创建人、重复报名、性别限制、信誉分门槛）
     * @param userProfile 用户档案领域对象
     */
    public void assertCanJoin(UserProfile userProfile) {
        assertCanJoin(userProfile, LocalDateTime.now());
    }

    public void assertCanJoin(UserProfile userProfile, LocalDateTime now) {
        String userId = userProfile.getUser().getUserId();
        MeetupStatusEnum realStatus = getRealStatus(now);

        // 1. 满员 / 约球状态校验
        if (isFull()) {
            throw new BusinessException(BizErrorCode.MEETUP_FULL);
        }
        if (realStatus == MeetupStatusEnum.CLOSED) {
            throw new BusinessException(BizErrorCode.MEETUP_CLOSED);
        }
        if (realStatus == MeetupStatusEnum.FINISHED) {
            throw new BusinessException(BizErrorCode.MEETUP_EXPIRED);
        }
        if (realStatus == MeetupStatusEnum.ONGOING) {
            throw new BusinessException(BizErrorCode.MEETUP_ONGOING);
        }

        // 2. 开始时间校验
        if (isExpired(now)) {
            throw new BusinessException(BizErrorCode.MEETUP_EXPIRED);
        }

        // 3. 不能报名自己的约球
        if (isCreator(userId)) {
            throw new BusinessException(BizErrorCode.CANNOT_JOIN_OWN);
        }

        // 4. 重复报名校验
        if (findActiveRegistration(userId) != null) {
            throw new BusinessException(BizErrorCode.ALREADY_JOINED);
        }

        // 5. 性别限制校验
        checkGenderLimit(userProfile);

        // 6. 信誉分门槛校验
        checkReputationScore(userProfile);

        // 7. 级别限制校验
        checkLevelLimit(userProfile);

    }

    private void checkLevelLimit(UserProfile userProfile) {
        if (!isLevelMatch(userProfile)) {
            throw new BusinessException(BizErrorCode.LEVEL_NOT_MATCH);
        }
    }

    /**
     * 性别限制校验
     * @param userProfile 用户档案领域对象
     */
    public void checkGenderLimit(UserProfile userProfile) {
        if (!isGenderMatch(userProfile)) {
            throw new BusinessException(BizErrorCode.GENDER_NOT_MATCH);
        }
    }

    /**
     * 信誉分门槛校验
     * @param userProfile 用户档案领域对象
     */
    public void checkReputationScore(UserProfile userProfile) {
        if (!isReputationOk(userProfile)) {
            throw new BusinessException(BizErrorCode.LOW_REPUTATION_BANNED);
        }
    }

    /** 水平是否符合（无要求或无档案数据时视为符合） */
    private boolean isLevelMatch(UserProfile userProfile) {
        if (data.getLevelMode() == null) {
            return true;
        }
        if (userProfile.getProfile() == null || userProfile.getProfile().getNtrpScore() == null) {
            return true;
        }
        BigDecimal userLevel = userProfile.getProfile().getNtrpScore();
        switch (data.getLevelMode()) {
            case RANGE:
                return !((data.getLevelMin() != null && userLevel.compareTo(data.getLevelMin()) < 0)
                        || (data.getLevelMax() != null && userLevel.compareTo(data.getLevelMax()) > 0));
            case EXACT:
                return data.getLevelMin() == null || userLevel.compareTo(data.getLevelMin()) == 0;
            case ABOVE:
                return data.getLevelMin() == null || userLevel.compareTo(data.getLevelMin()) >= 0;
            case BELOW:
                return data.getLevelMax() == null || userLevel.compareTo(data.getLevelMax()) <= 0;
            default:
                return true;
        }
    }

    /** 性别是否符合（不限或未知性别时视为符合） */
    private boolean isGenderMatch(UserProfile userProfile) {
        if (data.getGenderLimit() == GenderLimitEnum.ANY || userProfile.getUser().getGender() == null) {
            return true;
        }
        String userGender = userProfile.getUser().getGender().name();
        if (data.getGenderLimit() == GenderLimitEnum.MALE) {
            return "MALE".equals(userGender);
        }
        if (data.getGenderLimit() == GenderLimitEnum.FEMALE) {
            return "FEMALE".equals(userGender);
        }
        return true;
    }

    /** 信誉分是否达标（无档案数据时视为达标） */
    private boolean isReputationOk(UserProfile userProfile) {
        if (userProfile.getProfile() == null || userProfile.getProfile().getReputationScore() == null) {
            return true;
        }
        int threshold = SystemConfig.getInt(SystemConfigKey.MEETUP_JOIN_MIN_REPUTATION_SCORE.getKey());
        return userProfile.getProfile().getReputationScore() >= threshold;
    }

    /**
     * 收集未报名场景下的准入限制原因（满员/性别/水平/信誉分，可叠加）。
     * 仅当 actionState 为 JOIN_DIRECT/APPLY_APPROVAL 时由应用层调用；返回空列表表示可报名。
     * @param userProfile 当前用户档案领域对象
     */
    public List<JoinRestrictionEnum> collectJoinRestrictions(UserProfile userProfile) {
        List<JoinRestrictionEnum> restrictions = new ArrayList<>();
        // 检查用户信息完善状态
        boolean basicDefault = userProfile.getUser().isBasicDefault();
        boolean profileIncomplete = !userProfile.hasProfile();
        if (basicDefault && profileIncomplete) {
            restrictions.add(JoinRestrictionEnum.REGISTRATION_INCOMPLETE);
        } else if (basicDefault) {
            restrictions.add(JoinRestrictionEnum.PROFILE_INCOMPLETE);
        } else if (profileIncomplete) {
            restrictions.add(JoinRestrictionEnum.ONBOARDING_INCOMPLETE);
        }
        if (isFull()) {
            restrictions.add(JoinRestrictionEnum.FULL);
        }
        if (!isGenderMatch(userProfile)) {
            if (userProfile.getUser().getGender() == GenderEnum.UNDISCLOSED) {
                restrictions.add(JoinRestrictionEnum.GENDER_UNKNOWN);
            } else if (data.getGenderLimit() == GenderLimitEnum.MALE) {
                restrictions.add(JoinRestrictionEnum.GENDER_MALE_ONLY);
            } else if (data.getGenderLimit() == GenderLimitEnum.FEMALE) {
                restrictions.add(JoinRestrictionEnum.GENDER_FEMALE_ONLY);
            }
        }
        if (!isLevelMatch(userProfile)) {
            restrictions.add(JoinRestrictionEnum.LEVEL_NOT_MATCH);
        }
        if (!isReputationOk(userProfile)) {
            restrictions.add(JoinRestrictionEnum.LOW_REPUTATION);
        }
        return restrictions;
    }

    // ======================== 报名操作（需聚合根上下文） ========================

    /**
     * 退出报名（已加入 → QUIT）
     * @param userId 当前用户 ID
     * @return 退出结果（是否在 6h 内需扣分）
     */
    public QuitResult quit(String userId) {
        return quit(userId, LocalDateTime.now());
    }

    /** C8：普通约球的有效参与者可退出，创建者与活动时间均不构成额外限制。 */
    public QuitResult quit(String userId, LocalDateTime now) {
        if (MeetupTypeEnum.TOURNAMENT.getCode().equals(data.getMeetupType())) {
            throw new BusinessException(BizErrorCode.MEETUP_TOURNAMENT_QUIT_FORBIDDEN);
        }
        // 1. 查找报名记录并校验
        RegistrationData registration = findActiveRegistration(userId);
        Assert.notNull(registration, BizErrorCode.NOT_JOINED);
        Assert.isTrue(registration.canQuit(), BizErrorCode.NOT_JOINED);

        // 2. 更新状态
        registration.setStatus(RegistrationStatusEnum.QUIT);

        // 3. 判断是否在 6h 内
        long hoursUntilStart = Duration.between(now, data.getStartTime()).toHours();
        int thresholdHours = SystemConfig.getInt(SystemConfigKey.MEETUP_QUIT_PENALTY_THRESHOLD_HOURS.getKey());
        validateAfterCommand();
        return hoursUntilStart < thresholdHours ? QuitResult.PENALIZED : QuitResult.NORMAL;
    }

    /**
     * 审批通过
     * @param registrationId 报名记录 ID
     * @param currentUserId 当前用户 ID（审批人）
     */
    public String approve(String registrationId, String currentUserId) {
        return approve(registrationId, currentUserId, LocalDateTime.now());
    }

    /** C5：批准时不检查容量或 expiresAt，允许审批后达到或超过人数上限。 */
    public String approve(String registrationId, String currentUserId, LocalDateTime now) {
        // 1. 查找报名记录
        RegistrationData registration = findRegistrationById(registrationId);
        Assert.notNull(registration, BizErrorCode.WAITLIST_NOT_FOUND);

        // 2. 权限校验
        assertOwner(currentUserId);

        // 3. 状态校验
        registration.assertCanReview();
        Assert.isTrue(isActive(now), BizErrorCode.MEETUP_STATUS_ILLEGAL);

        // 4. 更新状态
        registration.setStatus(RegistrationStatusEnum.JOINED);

        validateAfterCommand();
        return registration.getUserId();
    }

    /**
     * 审批拒绝
     * @param registrationId 报名记录 ID
     * @param currentUserId 当前用户 ID（审批人）
     */
    public void reject(String registrationId, String currentUserId) {
        reject(registrationId, currentUserId, LocalDateTime.now());
    }

    /** C6：只校验创建者与 PENDING；不校验活动状态、加入模式或过期时间。 */
    public void reject(String registrationId, String currentUserId, LocalDateTime now) {
        // 1. 查找报名记录
        RegistrationData registration = findRegistrationById(registrationId);
        Assert.notNull(registration, BizErrorCode.WAITLIST_NOT_FOUND);

        // 2. 权限校验
        assertOwner(currentUserId);

        // 3. 状态校验
        registration.assertCanReview();

        // 4. 更新状态
        registration.setStatus(RegistrationStatusEnum.REJECTED);
        // 兼容现状：拒绝不补 optTime。
        validateAfterCommand();
    }

    /**
     * 邀请用户加入（创建人邀请，直接加入无需审批）
     * @param inviteeUserId 被邀请人用户ID
     * @param currentUserId 当前用户ID（邀请人）
     */
    public void invite(String inviteeUserId, String currentUserId) {
        // 1. 权限校验：必须是创建人
        assertOwner(currentUserId);

        // 2. 满员校验
        if (isFull()) {
            throw new BusinessException(BizErrorCode.MEETUP_FULL);
        }

        // 3. 重复报名校验
        if (findActiveRegistration(inviteeUserId) != null) {
            throw new BusinessException(BizErrorCode.ALREADY_JOINED);
        }

        // 4. 创建报名记录，直接设置为 JOINED
        RegistrationData registration = new RegistrationData();
        registration.setRallyMeetupId(data.getBizId());
        registration.setUserId(inviteeUserId);
        registration.setStatus(RegistrationStatusEnum.JOINED);
        this.registrations.add(registration);
        validateAfterCommand();
    }

    /** C7：撤回本人唯一的 PENDING 报名，保留历史报名且记录操作时间。 */
    public void withdraw(String userId, LocalDateTime now) {
        RegistrationData activeRegistration = findActiveRegistration(userId);
        Assert.notNull(activeRegistration, BizErrorCode.NOT_JOINED);
        Assert.isTrue(activeRegistration.isPending(), BizErrorCode.WAITLIST_NOT_PENDING);
        List<RegistrationData> pending = registrations.stream()
                .filter(r -> userId.equals(r.getUserId()) && r.isPending())
                .toList();
        Assert.isTrue(pending.size() == 1, BizErrorCode.WAITLIST_NOT_PENDING);
        RegistrationData registration = pending.get(0);
        registration.setStatus(RegistrationStatusEnum.WITHDRAWN);
        registration.setOptTime(now);
        validateAfterCommand();
    }

    /** 按 registrationId 在聚合根内查找报名记录 */
    private RegistrationData findRegistrationById(String registrationId) {
        return registrations.stream()
                .filter(r -> registrationId.equals(r.getBizId()))
                .findFirst().orElse(null);
    }

    /** C9：完成或跳过评价；完成评价时外部覆盖结论必须完整。 */
    public void completeReview(String userId, boolean skipped, boolean reviewCoverageComplete, LocalDateTime now) {
        RegistrationData registration = findActiveRegistration(userId);
        Assert.notNull(registration, BizErrorCode.NOT_JOINED);
        Assert.isTrue(registration.getStatus() == RegistrationStatusEnum.JOINED, BizErrorCode.MEETUP_CANT_REVIEW);

        MeetupStatusEnum realStatus = getRealStatus(now);
        Assert.isTrue(realStatus == MeetupStatusEnum.ONGOING || realStatus == MeetupStatusEnum.FINISHED,
                BizErrorCode.MEETUP_CANT_REVIEW);
        int deadlineDays = SystemConfig.getInt(SystemConfigKey.REVIEW_DEADLINE_DAYS.getKey());
        if (now.isAfter(data.getEndTime().plusDays(deadlineDays))) {
            throw new BusinessException(BizErrorCode.REVIEW_DEADLINE_PASSED);
        }
        if (!skipped && !reviewCoverageComplete) {
            throw new BusinessException(BizErrorCode.MEETUP_CANT_REVIEW);
        }

        registration.setStatus(skipped ? RegistrationStatusEnum.SKIPPED : RegistrationStatusEnum.REVIEWED);
        registration.setOptTime(now);
        validateAfterCommand();
    }

    /** C10：关闭普通约球；创建者退出不改变根上的创建者身份。 */
    public void close(String userId, LocalDateTime now) {
        assertOwner(userId);
        if (MeetupTypeEnum.TOURNAMENT.getCode().equals(data.getMeetupType())) {
            throw new BusinessException(BizErrorCode.MEETUP_TOURNAMENT_CLOSE_FORBIDDEN);
        }
        MeetupStatusEnum realStatus = getRealStatus(now);
        if (realStatus == MeetupStatusEnum.CLOSED
                || (hasOtherParticipants() && realStatus == MeetupStatusEnum.FINISHED)) {
            throw new BusinessException(BizErrorCode.MEETUP_STATUS_ILLEGAL);
        }
        data.setStatus(MeetupStatusEnum.CLOSED);
        validateAfterCommand();
    }

    /** C11：比赛全员确认后开放仍未开始的赛事草稿。 */
    public void openTournamentDraft(boolean allParticipantsConfirmed, LocalDateTime now) {
        Assert.isTrue(MeetupTypeEnum.TOURNAMENT.getCode().equals(data.getMeetupType()),
                BizErrorCode.MEETUP_STATUS_ILLEGAL);
        Assert.isTrue(allParticipantsConfirmed, BizErrorCode.MEETUP_STATUS_ILLEGAL);
        Assert.isTrue(data.getStatus() == MeetupStatusEnum.DRAFT, BizErrorCode.MEETUP_STATUS_ILLEGAL);
        assertNotExpired(now);
        // FULL 仅为历史枚举概念；持久化状态按 I6 始终写 OPEN。
        data.setStatus(MeetupStatusEnum.OPEN);
        validateAfterCommand();
    }

    /** C12：关联比赛终止后关闭赛事草稿。 */
    public void closeTournamentDraft(boolean linkedMatchTerminated) {
        Assert.isTrue(MeetupTypeEnum.TOURNAMENT.getCode().equals(data.getMeetupType()),
                BizErrorCode.MEETUP_STATUS_ILLEGAL);
        Assert.isTrue(linkedMatchTerminated, BizErrorCode.MEETUP_STATUS_ILLEGAL);
        Assert.isTrue(data.getStatus() == MeetupStatusEnum.DRAFT, BizErrorCode.MEETUP_STATUS_ILLEGAL);
        data.setStatus(MeetupStatusEnum.CLOSED);
        validateAfterCommand();
    }

    /** C13：到达结束时刻后显式结算为 FINISHED。 */
    public void finish(LocalDateTime now) {
        MeetupStatusEnum storedStatus = data.getStatus();
        Assert.isTrue(storedStatus == MeetupStatusEnum.OPEN || storedStatus == MeetupStatusEnum.ONGOING,
                BizErrorCode.MEETUP_STATUS_ILLEGAL);
        if (now.isBefore(data.getEndTime())) {
            throw new BusinessException(BizErrorCode.MEETUP_STATUS_ILLEGAL);
        }
        data.setStatus(MeetupStatusEnum.FINISHED);
        validateAfterCommand();
    }

    /** C14：原子替换完整费用方案，空列表表示清空相应部分。 */
    public void replaceCost(String userId, List<CostItem> costItems,
                            List<HourlyAllocation> hourlyAllocations) {
        assertOwner(userId);
        validateHourlyAllocations(hourlyAllocations);

        CostData costData = new CostData();
        costData.setCostItems(costItems);
        costData.setHourlyAllocations(hourlyAllocations);
        data.setCostData(costData);
        validateAfterCommand();
    }

    private void validateHourlyAllocations(List<HourlyAllocation> hourlyAllocations) {
        if (hourlyAllocations == null || hourlyAllocations.isEmpty()) {
            return;
        }

        // 保留既有求和顺序：duration 为 null 会在这里自然抛出系统异常，
        // 不转换成 PARAM_ERROR。
        BigDecimal allocatedDuration = hourlyAllocations.stream()
                .map(HourlyAllocation::getDuration)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (allocatedDuration.compareTo(data.getDuration()) != 0) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "MEETUP_COST_INVALID");
        }

        for (HourlyAllocation allocation : hourlyAllocations) {
            if (allocation.getDuration().compareTo(BigDecimal.ZERO) <= 0
                    || allocation.getUserIds() == null || allocation.getUserIds().isEmpty()) {
                throw new BusinessException(BizErrorCode.PARAM_ERROR, "MEETUP_COST_INVALID");
            }
            for (String allocationUserId : allocation.getUserIds()) {
                if (!isParticipant(allocationUserId)) {
                    throw new BusinessException(BizErrorCode.PARAM_ERROR, "MEETUP_COST_INVALID");
                }
            }
        }
    }

    /** I1/I2/I4/I6 的聚合写后守卫。 */
    public void validateAfterCommand() {
        validateIdentity();
        recalculateCurrentPlayers();
        validateTimeProjection();
    }

    private void validateIdentity() {
        if (isBlank(data.getBizId()) || !Objects.equals(loadedMeetupId, data.getBizId())) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "MEETUP_IDENTITY_INVALID");
        }
        Set<String> registrationIds = new HashSet<>();
        for (RegistrationData registration : registrations) {
            if (registration == null || isBlank(registration.getBizId())
                    || !Objects.equals(data.getBizId(), registration.getRallyMeetupId())
                    || (loadedRegistrationIds.containsKey(registration)
                    && (!Objects.equals(loadedRegistrationIds.get(registration), registration.getBizId())
                    || !Objects.equals(loadedRegistrationMeetupIds.get(registration), registration.getRallyMeetupId())))
                    || !registrationIds.add(registration.getBizId())) {
                throw new BusinessException(BizErrorCode.PARAM_ERROR, "MEETUP_IDENTITY_INVALID");
            }
        }
    }

    private void validateTimeProjection() {
        if (data.getStartTime() == null || data.getEndTime() == null || data.getDuration() == null
                || data.getDuration().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR);
        }
        LocalDateTime expectedEnd = data.getStartTime()
                .plusHours(data.getDuration().longValue())
                .plusMinutes(data.getDuration().remainder(BigDecimal.ONE)
                        .multiply(BigDecimal.valueOf(60)).longValue());
        if (!expectedEnd.equals(data.getEndTime())) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // ======================== 操作状态计算 ========================

    /**
     * 计算当前用户的操作状态（沉淀到聚合根，内部已有所有数据）
     * @param currentUserId 当前用户 ID
     * @return 操作状态枚举
     */
    public ActionStateEnum getActionState(String currentUserId) {
        MeetupStatusEnum realStatus = getRealStatus();
        boolean isCreator = isCreator(currentUserId);

        // CLOSED 状态优先（无论是否有其他参与者）
        if (realStatus == MeetupStatusEnum.CLOSED) {
            return isParticipant(currentUserId) ? ActionStateEnum.CLOSED_JOINED : ActionStateEnum.CLOSED;
        }

        // 创建人 + 无其他参与者：可编辑（即使 FINISHED 也可编辑）
        if (isCreator && !hasOtherParticipants()) {
            return ActionStateEnum.OWNER_EDITABLE;
        }

        // FINISHED 状态（此时必有其他参与者）
        if (realStatus == MeetupStatusEnum.FINISHED) {
            if (!isParticipant(currentUserId)) return ActionStateEnum.FINISHED;
            return hasReview(currentUserId) ? ActionStateEnum.FINISHED_REVIEWED : ActionStateEnum.FINISHED_JOINED;
        }

        // 创建人视角（此时必有其他参与者）
        if (isCreator) {
            if (realStatus == MeetupStatusEnum.ONGOING) {
                return ActionStateEnum.ONGOING_JOINED;
            }
            int lockMinutes = SystemConfig.getInt(SystemConfigKey.MEETUP_EDIT_LOCK_MINUTES_BEFORE_START.getKey());
            boolean locked = LocalDateTime.now().isAfter(data.getStartTime().minusMinutes(lockMinutes));
            return locked ? ActionStateEnum.OWNER_EDIT_LOCKED : ActionStateEnum.OWNER_EDITABLE;
        }

        // 访客视角：根据报名状态判断
        RegistrationData userRegistration = findActiveRegistration(currentUserId);
        if (userRegistration != null) {
            if (userRegistration.isPending()) {
                return ActionStateEnum.PENDING_REVIEW;
            }
            if (userRegistration.isActiveParticipant()) {
                return realStatus == MeetupStatusEnum.ONGOING ? ActionStateEnum.ONGOING_JOINED : ActionStateEnum.JOINED;
            }
        }

        // 未报名根据加入模式判断（满员不再单列状态，由 collectJoinRestrictions 体现为 FULL 限制）
        if (realStatus == MeetupStatusEnum.ONGOING) {
            return ActionStateEnum.ONGOING;
        }
        return data.getJoinMode() == JoinModeEnum.DIRECT
                ? ActionStateEnum.JOIN_DIRECT : ActionStateEnum.APPLY_APPROVAL;
    }



    /**
     * 按视角获取报名参与者记录
     * <ul>
     *   <li>创建人视角：已批准 + 待审批</li>
     *   <li>非创建人视角：仅已批准</li>
     * </ul>
     * @param userId 当前用户 ID，内部判断是否为创建人
     * @return 报名参与者记录列表
     */
    public List<RegistrationData> getParticipants(String userId) {
        boolean creator = isCreator(userId);
        return registrations.stream()
                .filter(r -> r.isActiveParticipant() || (creator && r.isPending()))
                .toList();
    }

    /**
     * 获取除指定用户外的全部有效参与者 userId（用于群发通知，excludeUserId 传 null 表示全部）
     */
    public List<String> getActiveParticipantIds(String excludeUserId) {
        return registrations.stream().filter(RegistrationData::isActiveParticipant).map(RegistrationData::getUserId).filter(uid -> !uid.equals(excludeUserId)).toList();
    }

    public List<String> getReviewWaitlistIds(String userId) {
        List<String> res = new ArrayList<>();
        for (RegistrationData r : registrations) {
            if (r.getUserId().equals(userId)) {
                continue;

            }
            if (r.isActiveParticipant()) {
                res.add(r.getUserId());
            }
        }
        return res;
    }

    /** 用户是否已完成评价（REVIEWED 或 SKIPPED，用于短路判断） */
    public boolean hasReview(String userId) {
        return registrations.stream()
                .filter(r -> userId.equals(r.getUserId()))
                .anyMatch(r -> r.getStatus() == RegistrationStatusEnum.REVIEWED || r.getStatus() == RegistrationStatusEnum.SKIPPED);
    }

    public void assertCanReview() {
        Assert.isTrue(canReview(), BizErrorCode.MEETUP_CANT_REVIEW);
    }

    public boolean canReview() {
        MeetupStatusEnum realStatus = getRealStatus();
        return realStatus == MeetupStatusEnum.FINISHED || realStatus == MeetupStatusEnum.ONGOING;
    }

    /**
     * 比分与逐项评价入口的共享资格断言。PENDING 经 assertIn 通过，
     * 再与其他活跃报名一样受 ONGOING/FINISHED 阶段和评价截止时间约束。
     * C9 完成或跳过评价的报名状态迁移仍由 completeReview 单独限定为 JOINED。
     */
    public void assertReviewAvailable(String userId) {
        assertIn(userId);
        assertCanReview();
        int deadlineDays = SystemConfig.getInt(SystemConfigKey.REVIEW_DEADLINE_DAYS.getKey());
        LocalDateTime deadlineAt = this.getData().getEndTime().plusDays(deadlineDays);
        if (LocalDateTime.now().isAfter(deadlineAt)) {
            throw new BusinessException(BizErrorCode.REVIEW_DEADLINE_PASSED);
        }
    }

    /** 聊天资格与 {@link #assertIn(String)} 保持一致，PENDING 也是聊天活跃报名。 */
    public boolean canChat(String userId) {
        return isCreator(userId) || findActiveRegistration(userId) != null;
    }
}
