package com.piania.core.service;

import com.piania.core.entity.ShareLink;
import com.piania.core.entity.ShareLinkPermission;
import com.piania.core.entity.SheetMusic;
import com.piania.core.enums.ShareAccessType;
import com.piania.core.exception.ForbiddenException;
import com.piania.core.exception.NotFoundException;
import com.piania.core.repository.ShareLinkPermissionRepository;
import com.piania.core.repository.ShareLinkRepository;
import com.piania.core.repository.SheetMusicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShareLinkServiceTest {

    @Mock
    private ShareLinkRepository shareLinkRepository;

    @Mock
    private ShareLinkPermissionRepository permissionRepository;

    @Mock
    private SheetMusicRepository sheetMusicRepository;

    @InjectMocks
    private ShareLinkService service;

    @Captor
    private ArgumentCaptor<ShareLink> shareLinkCaptor;

    private SheetMusic sheetMusic;
    private final String ownerEmail = "owner@piania.com";

    @BeforeEach
    void setUp() {
        sheetMusic = SheetMusic.builder()
                .id(10L)
                .ownerEmail(ownerEmail)
                .title("Nocturne")
                .build();
    }

    @Test
    void createShareLink_whenSheetMusicNotFound_throwsNotFound() {
        when(sheetMusicRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createShareLink(10L, ShareAccessType.PUBLIC, null, ownerEmail))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Sheet music not found");

        verifyNoInteractions(shareLinkRepository);
    }

    @Test
    void createShareLink_whenNotOwner_throwsForbidden() {
        when(sheetMusicRepository.findById(10L)).thenReturn(Optional.of(sheetMusic));

        assertThatThrownBy(() -> service.createShareLink(10L, ShareAccessType.PUBLIC, null, "attacker@piania.com"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not the owner");

        verifyNoInteractions(shareLinkRepository);
    }

    @Test
    void createShareLink_whenValid_persistsAndReturns() {
        when(sheetMusicRepository.findById(10L)).thenReturn(Optional.of(sheetMusic));
        when(shareLinkRepository.save(any(ShareLink.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime expires = LocalDateTime.now().plusDays(3);

        ShareLink created = service.createShareLink(10L, ShareAccessType.RESTRICTED, expires, ownerEmail);

        assertThat(created.getOwnerEmail()).isEqualTo(ownerEmail);
        assertThat(created.getSheetMusic()).isSameAs(sheetMusic);
        assertThat(created.getAccessType()).isEqualTo(ShareAccessType.RESTRICTED);
        assertThat(created.getExpiresAt()).isEqualTo(expires);
        assertThat(created.isActive()).isTrue();

        verify(shareLinkRepository).save(any(ShareLink.class));
    }

    @Test
    void accessByToken_whenNotFound_throwsNotFound() {
        when(shareLinkRepository.findByToken("t")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.accessByToken("t", "x@piania.com"))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(permissionRepository);
    }

    @Test
    void accessByToken_whenInactive_throwsForbidden() {
        ShareLink shareLink = ShareLink.builder()
                .id(1L)
                .token("t")
                .active(false)
                .accessType(ShareAccessType.PUBLIC)
                .sheetMusic(sheetMusic)
                .ownerEmail(ownerEmail)
                .build();

        when(shareLinkRepository.findByToken("t")).thenReturn(Optional.of(shareLink));

        assertThatThrownBy(() -> service.accessByToken("t", "x@piania.com"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void accessByToken_whenExpired_throwsForbidden() {
        ShareLink shareLink = ShareLink.builder()
                .id(1L)
                .token("t")
                .active(true)
                .accessType(ShareAccessType.PUBLIC)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .sheetMusic(sheetMusic)
                .ownerEmail(ownerEmail)
                .build();

        when(shareLinkRepository.findByToken("t")).thenReturn(Optional.of(shareLink));

        assertThatThrownBy(() -> service.accessByToken("t", "x@piania.com"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void accessByToken_public_returnsSheetMusic() {
        ShareLink shareLink = ShareLink.builder()
                .id(1L)
                .token("t")
                .active(true)
                .accessType(ShareAccessType.PUBLIC)
                .sheetMusic(sheetMusic)
                .ownerEmail(ownerEmail)
                .build();

        when(shareLinkRepository.findByToken("t")).thenReturn(Optional.of(shareLink));

        SheetMusic result = service.accessByToken("t", "someone@piania.com");

        assertThat(result).isSameAs(sheetMusic);
        verifyNoInteractions(permissionRepository);
    }

    @Test
    void accessByToken_private_whenNotOwner_throwsForbidden() {
        ShareLink shareLink = ShareLink.builder()
                .id(1L)
                .token("t")
                .active(true)
                .accessType(ShareAccessType.PRIVATE)
                .sheetMusic(sheetMusic)
                .ownerEmail(ownerEmail)
                .build();

        when(shareLinkRepository.findByToken("t")).thenReturn(Optional.of(shareLink));

        assertThatThrownBy(() -> service.accessByToken("t", "other@piania.com"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void accessByToken_private_whenOwner_returnsSheetMusic() {
        ShareLink shareLink = ShareLink.builder()
                .id(1L)
                .token("t")
                .active(true)
                .accessType(ShareAccessType.PRIVATE)
                .sheetMusic(sheetMusic)
                .ownerEmail(ownerEmail)
                .build();

        when(shareLinkRepository.findByToken("t")).thenReturn(Optional.of(shareLink));

        SheetMusic result = service.accessByToken("t", ownerEmail);

        assertThat(result).isSameAs(sheetMusic);
    }

    @Test
    void accessByToken_restricted_whenOwner_returnsSheetMusic() {
        ShareLink shareLink = ShareLink.builder()
                .id(1L)
                .token("t")
                .active(true)
                .accessType(ShareAccessType.RESTRICTED)
                .sheetMusic(sheetMusic)
                .ownerEmail(ownerEmail)
                .build();

        when(shareLinkRepository.findByToken("t")).thenReturn(Optional.of(shareLink));

        SheetMusic result = service.accessByToken("t", ownerEmail);

        assertThat(result).isSameAs(sheetMusic);
        verifyNoInteractions(permissionRepository);
    }

    @Test
    void accessByToken_restricted_whenNotAllowed_throwsForbidden() {
        ShareLink shareLink = ShareLink.builder()
                .id(1L)
                .token("t")
                .active(true)
                .accessType(ShareAccessType.RESTRICTED)
                .sheetMusic(sheetMusic)
                .ownerEmail(ownerEmail)
                .build();

        when(shareLinkRepository.findByToken("t")).thenReturn(Optional.of(shareLink));
        when(permissionRepository.findByShareLinkIdAndAllowedEmail(1L, "a@piania.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.accessByToken("t", "a@piania.com"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void accessByToken_restricted_whenAllowed_returnsSheetMusic() {
        ShareLink shareLink = ShareLink.builder()
                .id(1L)
                .token("t")
                .active(true)
                .accessType(ShareAccessType.RESTRICTED)
                .sheetMusic(sheetMusic)
                .ownerEmail(ownerEmail)
                .build();

        when(shareLinkRepository.findByToken("t")).thenReturn(Optional.of(shareLink));
        when(permissionRepository.findByShareLinkIdAndAllowedEmail(1L, "a@piania.com"))
                .thenReturn(Optional.of(ShareLinkPermission.builder().id(99L).allowedEmail("a@piania.com").shareLink(shareLink).build()));

        SheetMusic result = service.accessByToken("t", "a@piania.com");

        assertThat(result).isSameAs(sheetMusic);
    }

    @Test
    void revokeShareLink_whenNotFound_throwsNotFound() {
        when(shareLinkRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revokeShareLink(5L, ownerEmail))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void revokeShareLink_whenNotOwner_throwsForbidden() {
        ShareLink shareLink = ShareLink.builder()
                .id(5L)
                .ownerEmail(ownerEmail)
                .active(true)
                .build();
        when(shareLinkRepository.findById(5L)).thenReturn(Optional.of(shareLink));

        assertThatThrownBy(() -> service.revokeShareLink(5L, "attacker@piania.com"))
                .isInstanceOf(ForbiddenException.class);

        verify(shareLinkRepository, never()).save(any());
    }

    @Test
    void revokeShareLink_whenOwner_setsInactive() {
        ShareLink shareLink = ShareLink.builder()
                .id(5L)
                .ownerEmail(ownerEmail)
                .active(true)
                .build();
        when(shareLinkRepository.findById(5L)).thenReturn(Optional.of(shareLink));

        service.revokeShareLink(5L, ownerEmail);

        assertThat(shareLink.isActive()).isFalse();
        verify(shareLinkRepository).save(shareLink);
    }

    @Test
    void addPermission_whenNotRestricted_throwsForbidden() {
        ShareLink shareLink = ShareLink.builder()
                .id(5L)
                .ownerEmail(ownerEmail)
                .active(true)
                .accessType(ShareAccessType.PUBLIC)
                .build();
        when(shareLinkRepository.findById(5L)).thenReturn(Optional.of(shareLink));

        assertThatThrownBy(() -> service.addPermission(5L, "a@piania.com", ownerEmail))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Permissions only allowed");

        verifyNoInteractions(permissionRepository);
    }

    @Test
    void addPermission_whenRestrictedAndOwner_persistsPermission() {
        ShareLink shareLink = ShareLink.builder()
                .id(5L)
                .ownerEmail(ownerEmail)
                .active(true)
                .accessType(ShareAccessType.RESTRICTED)
                .build();
        when(shareLinkRepository.findById(5L)).thenReturn(Optional.of(shareLink));

        service.addPermission(5L, "allowed@piania.com", ownerEmail);

        ArgumentCaptor<ShareLinkPermission> captor = ArgumentCaptor.forClass(ShareLinkPermission.class);
        verify(permissionRepository).save(captor.capture());
        assertThat(captor.getValue().getAllowedEmail()).isEqualTo("allowed@piania.com");
        assertThat(captor.getValue().getShareLink()).isSameAs(shareLink);
    }

    @Test
    void getMyActiveLinks_returnsRepositoryResult() {
        ShareLink l1 = ShareLink.builder().id(1L).ownerEmail(ownerEmail).active(true).build();
        when(shareLinkRepository.findByOwnerEmailAndActiveTrue(ownerEmail)).thenReturn(List.of(l1));

        List<ShareLink> result = service.getMyActiveLinks(ownerEmail);

        assertThat(result).containsExactly(l1);
    }
}
