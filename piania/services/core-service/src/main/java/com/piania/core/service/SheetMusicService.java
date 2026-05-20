package com.piania.core.service;

import com.piania.core.dto.sheetmusic.SheetMusicRequest;
import com.piania.core.entity.SheetMusic;
import com.piania.core.enums.SheetMusicStatus;
import com.piania.core.exception.ForbiddenException;
import com.piania.core.exception.NotFoundException;
import com.piania.core.repository.SheetMusicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SheetMusicService {

    private final SheetMusicRepository sheetMusicRepository;

    public SheetMusic create(String userEmail, SheetMusicRequest request) {

        SheetMusic sheetMusic = SheetMusic.builder()
                .title(request.getTitle())
                .composer(request.getComposer())
                .ownerEmail(userEmail)
                .originalFileUrl(request.getOriginalFileUrl())
                .status(SheetMusicStatus.UPLOADED)
                .isPublic(request.getIsPublic())
                .deleted(false)
                .build();

        return sheetMusicRepository.save(sheetMusic);
    }

    public Page<SheetMusic> getUserSheetMusic(String userEmail, Pageable pageable) {
        return sheetMusicRepository.findByOwnerEmailAndDeletedFalse(userEmail, pageable);
    }

    public SheetMusic getById(Long id) {
        return sheetMusicRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sheet music not found"));
    }

    public SheetMusic getAccessibleById(Long id, String userEmail) {
        SheetMusic sheetMusic = getById(id);

        if (!sheetMusic.getOwnerEmail().equals(userEmail) && !sheetMusic.isPublic()) {
            throw new ForbiddenException("Access denied");
        }

        return sheetMusic;
    }

    public SheetMusic getOwnedById(Long id, String userEmail) {
        SheetMusic sheetMusic = getById(id);

        if (!sheetMusic.getOwnerEmail().equals(userEmail)) {
            throw new NotFoundException("Sheet music not found");
        }

        return sheetMusic;
    }

    public void delete(Long id, String userEmail) {

        SheetMusic sheetMusic = getOwnedById(id, userEmail);

        sheetMusic.setDeleted(true);
        sheetMusicRepository.save(sheetMusic);
    }
}
