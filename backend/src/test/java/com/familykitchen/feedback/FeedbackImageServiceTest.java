package com.familykitchen.feedback;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.util.Set;
import java.time.LocalDateTime;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import com.familykitchen.common.security.CurrentUserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FeedbackImageServiceTest {
  @TempDir Path temp;
  final FeedbackMapper mapper=mock(FeedbackMapper.class);
  final CurrentUserContext user=new CurrentUserContext(1L,null,null,1L,"user",Set.of(),Set.of());
  byte[] png() throws Exception {var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,3,BufferedImage.TYPE_INT_RGB),"png",out);return out.toByteArray();}
  FeedbackImageService service() throws Exception {return new FeedbackImageService(mapper,new TestTransactions(),temp.resolve("private").toString(),temp.resolve("public").toString());}
  @Test void rejectsPrivateDirectoryInsidePublicRoot() {
    assertThatThrownBy(()->new FeedbackImageService(mapper,new TestTransactions(),temp.resolve("public/private").toString(),temp.resolve("public").toString())).hasMessageContaining("公开");
  }
  @Test void checksQuotaBeforeReadingOrDecoding() throws Exception {
    when(mapper.lockUser(1L)).thenReturn(1L);when(mapper.countRecentImages(eq(1L),any())).thenReturn(30);
    var file=mock(org.springframework.web.multipart.MultipartFile.class);
    assertThatThrownBy(()->service().upload(user,file)).hasMessageContaining("30");verifyNoInteractions(file);
  }
  @Test void uploadsPrivatePngAndCompensatesRegistrationFailure() throws Exception {
    when(mapper.lockUser(1L)).thenReturn(1L);
    when(mapper.insertImage(any())).thenThrow(new IllegalStateException("database unavailable"));
    assertThatThrownBy(()->service().upload(user,new MockMultipartFile("file","x.png","image/png",png()))).hasMessageContaining("database");
    try(var files=Files.list(temp.resolve("private"))){assertThat(files.count()).isZero();}
  }
  @Test void rejectsMimeSignatureMismatch() throws Exception {
    when(mapper.lockUser(1L)).thenReturn(1L);
    assertThatThrownBy(()->service().upload(user,new MockMultipartFile("file","x.jpg","image/jpeg",png()))).hasMessageContaining("格式");
  }
  @Test void adminCannotReadUnboundAttachmentButOwnerCan() throws Exception {
    var service=service();var image=new FeedbackImage();image.id="id";image.ownerUserId=1L;image.objectKey="a.png";image.mime="image/png";image.createdAt=LocalDateTime.now();
    Files.write(temp.resolve("private/a.png"),png());when(mapper.image("id")).thenReturn(image);
    var admin=new CurrentUserContext(2L,null,null,2L,"user",Set.of("PLATFORM_ADMIN"),Set.of());
    assertThatThrownBy(()->service.read(admin,"id")).hasMessageContaining("不存在");
    assertThat(service.read(user,"id").bytes()).isEqualTo(png());image.feedbackId=3L;
    assertThat(service.read(admin,"id").mime()).isEqualTo("image/png");
  }
  @Test void boundarySizeAllowsExactlyFourMiBButRejectsOneMore() throws Exception {
    when(mapper.lockUser(1L)).thenReturn(1L);when(mapper.insertImage(any())).thenReturn(1);
    var service=service();byte[] padded=java.util.Arrays.copyOf(png(),4*1024*1024);
    assertThat(service.upload(user,new MockMultipartFile("file","ignored.exe","image/png",padded))).containsKey("imageId");
    assertThatThrownBy(()->service.upload(user,new MockMultipartFile("file","x.png","image/png",java.util.Arrays.copyOf(padded,padded.length+1)))).hasMessageContaining("4 MiB");
  }
  @Test void rejectsHugeDimensionsBeforeDecodeAndTruncatedPng() throws Exception {
    when(mapper.lockUser(1L)).thenReturn(1L);var service=service();byte[] huge=png();
    java.nio.ByteBuffer.wrap(huge,16,8).putInt(100000).putInt(100000);
    assertThatThrownBy(()->service.upload(user,new MockMultipartFile("file","x.png","image/png",huge))).hasMessageContaining("像素");
    assertThatThrownBy(()->service.upload(user,new MockMultipartFile("file","x.png","image/png",java.util.Arrays.copyOf(png(),33)))).isInstanceOf(com.familykitchen.common.error.BusinessException.class);
  }
  @Test void retriesOldUnregisteredFilesWithoutTouchingRegisteredImages() throws Exception {
    var service=service();String key="aabbccdd-1111-2222-3333-123456789abc.png";
    Path orphan=temp.resolve("private/"+key);Files.write(orphan,png());Files.setLastModifiedTime(orphan,java.nio.file.attribute.FileTime.from(java.time.Instant.now().minusSeconds(90000)));
    Path unrelated=temp.resolve("private/unrelated.txt");Files.writeString(unrelated,"keep");
    service.cleanupExpired();assertThat(Files.exists(orphan)).isFalse();assertThat(Files.exists(unrelated)).isTrue();
  }
  static class TestTransactions extends org.springframework.transaction.support.AbstractPlatformTransactionManager {
    protected Object doGetTransaction(){return new Object();}
    protected void doBegin(Object t,org.springframework.transaction.TransactionDefinition d){}
    protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus s){}
    protected void doRollback(org.springframework.transaction.support.DefaultTransactionStatus s){}
  }
}
