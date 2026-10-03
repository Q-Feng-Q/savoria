package com.familykitchen.notebook;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.familykitchen.notebook.mapper.NotebookImageMapper;
import com.familykitchen.notebook.mapper.NotebookRecordMapper;
import com.familykitchen.notebook.service.NotebookAccessPolicy;
import com.familykitchen.notebook.service.NotebookImageService;
import com.familykitchen.notebook.service.NotebookAuditService;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Instant;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.mock.web.MockMultipartFile;

/** Private image requests cannot bypass record authorization. */
class NotebookImageAccessTest {
  private Path temp;

  @AfterEach void cleanup() throws Exception {
    if (temp == null) return;
    try (var paths = Files.walk(temp)) {
      for (var path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
    }
  }

  @Test void createOnlyCollaboratorCanStagePrivateImageBeforeRecordExists() throws Exception {
    temp = Files.createTempDirectory(Path.of("target").toAbsolutePath(), "notebook-image-");
    var images = mock(NotebookImageMapper.class);
    var access = mock(NotebookAccessPolicy.class);
    when(access.requireCreateCapability(2, 7)).thenReturn(
        new NotebookAccessPolicy.Scope(1, null, null, false));
    when(images.stage(eq(7L), eq(1L), eq(2L), anyString(), eq("photo.png"),
        eq("image/png"), anyLong(), any())).thenReturn(14L);
    var bytes = new ByteArrayOutputStream();
    org.assertj.core.api.Assertions.assertThat(ImageIO.write(
        new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "png", bytes)).isTrue();
    org.assertj.core.api.Assertions.assertThat(ImageIO.read(
        new java.io.ByteArrayInputStream(bytes.toByteArray()))).isNotNull();
    var service = new NotebookImageService(images, mock(NotebookRecordMapper.class), access,
        mock(NotebookAuditService.class), temp.resolve("private"), temp.resolve("public"));
    var staged = service.stage(2, 7,
        new MockMultipartFile("file", "photo.png", "image/png", bytes.toByteArray()));
    org.assertj.core.api.Assertions.assertThat(staged.imageId()).isEqualTo(14L);
    org.assertj.core.api.Assertions.assertThat(Files.exists(temp.resolve("private").resolve(staged.valueKey())))
        .isTrue();
    verify(access, never()).requireEdit(anyLong(), anyLong(), any(), any());
  }

  @Test void expiredUnboundUploadMovesToDurableCleanupQueue() {
    var images = mock(NotebookImageMapper.class);
    var expired = new NotebookImageMapper.Staged(3, 7, 1, 2,
        "00000000-0000-0000-0000-000000000003.png", "photo.png", "image/png", 12);
    when(images.expired(100)).thenReturn(java.util.List.of(expired));
    var service = new NotebookImageService(images, mock(NotebookRecordMapper.class),
        mock(NotebookAccessPolicy.class), mock(NotebookAuditService.class),
        Path.of("target/notebook-private"), Path.of("target/notebook-public"));
    service.expireStaged();
    verify(images).expire(expired);
  }
  @Test void unknownImageDoesNotReadPrivateStorage() {
    var service = new NotebookImageService(mock(NotebookImageMapper.class),
        mock(NotebookRecordMapper.class), mock(NotebookAccessPolicy.class),
        mock(NotebookAuditService.class), Path.of("data/notebook-private"), Path.of("data/uploads"));
    assertThatThrownBy(() -> service.read(2, 999)).isInstanceOf(RuntimeException.class);
  }

  @Test void existingImageStillRequiresCompleteRecordReadAuthorization() {
    var images = mock(NotebookImageMapper.class);
    var records = mock(NotebookRecordMapper.class);
    var access = mock(NotebookAccessPolicy.class);
    var audit = mock(NotebookAuditService.class);
    when(images.find(5)).thenReturn(new NotebookImageMapper.Image(5, 9, 1,
        "00000000-0000-0000-0000-000000000001.png", "photo.png", "image/png", 12));
    Instant instant = Instant.parse("2026-01-03T00:00:00Z");
    when(records.findAny(9)).thenReturn(new NotebookRecordMapper.Row(9, 7, 1, 1, 1,
        instant, instant, "A", null, 1, "{}", 0));
    when(access.requireRecordRead(2, 7, instant, instant)).thenThrow(
        new com.familykitchen.common.error.BusinessException(
            com.familykitchen.common.error.ErrorCode.NOT_FOUND, "denied"));
    var service = new NotebookImageService(images, records, access, audit,
        Path.of("data/notebook-private"), Path.of("data/uploads"));
    assertThatThrownBy(() -> service.read(2, 5)).isInstanceOf(RuntimeException.class);
    verifyNoInteractions(audit);
  }

  @Test void referencedImageCannotBeDeletedIntoADanglingValue() {
    var images = mock(NotebookImageMapper.class);
    var records = mock(NotebookRecordMapper.class);
    var access = mock(NotebookAccessPolicy.class);
    var audit = mock(NotebookAuditService.class);
    String key = "00000000-0000-0000-0000-000000000001.png";
    when(images.find(5)).thenReturn(new NotebookImageMapper.Image(5, 9, 1, key,
        "photo.png", "image/png", 12));
    Instant instant = Instant.parse("2026-01-03T00:00:00Z");
    when(records.lockAny(9)).thenReturn(new NotebookRecordMapper.Row(9, 7, 1, 1, 1,
        instant, instant, "A", null, 1, "{\"photo\":[\"" + key + "\"]}", 0));
    var service = new NotebookImageService(images, records, access, audit,
        Path.of("data/notebook-private"), Path.of("data/uploads"));
    assertThatThrownBy(() -> service.delete(1, 5)).isInstanceOf(
        com.familykitchen.common.error.BusinessException.class);
    verify(images, never()).delete(5);
  }
}
