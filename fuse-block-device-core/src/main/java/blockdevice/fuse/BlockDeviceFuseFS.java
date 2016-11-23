package blockdevice.fuse;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

import jnr.ffi.Pointer;
import jnr.ffi.types.mode_t;
import jnr.ffi.types.off_t;
import jnr.ffi.types.size_t;
import ru.serce.jnrfuse.ErrorCodes;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.FuseStubFS;
import ru.serce.jnrfuse.struct.FileStat;
import ru.serce.jnrfuse.struct.FuseFileInfo;

public class BlockDeviceFuseFS extends FuseStubFS {

  private static final Logger LOG = LoggerFactory.getLogger(BlockDeviceFuseFS.class);

  private static final String PATH_SEP = "/";
  private static final String EMPTY_STRING = "";
  private static final String DOT = ".";
  private static final String DOTDOT = DOT + DOT;
  private static final Splitter PATH_SPLITTER = Splitter.on('/');

  private static final boolean DEBUG = true;

  private final BlockDeviceManager blockDeviceManager;

  public BlockDeviceFuseFS(BlockDeviceManager blockDeviceManager) {
    this.blockDeviceManager = blockDeviceManager;
  }

  @Override
  public int open(String path, FuseFileInfo fi) {
    LOG.info("open path {}", path);
    try {
      String blockDeviceId = getBlockDeviceId(path);
      BlockDevice blockDevice = blockDeviceManager.open(blockDeviceId);
      if (blockDevice == null) {
        return -ErrorCodes.ENOENT();
      }
      fi.fh.set(blockDevice.getFileHandle());
      return 0;
    } catch (Throwable t) {
      LOG.error(t.getMessage(), t);
      return -ErrorCodes.EIO();
    }
  }

  @Override
  public int fsync(String path, int isdatasync, FuseFileInfo fi) {
    long fileHandle = fi.fh.get();
    LOG.info("fsync fh {} path {}", fileHandle, path);
    try {
      BlockDevice blockDevice = blockDeviceManager.lookup(fileHandle);
      if (blockDevice == null) {
        return -ErrorCodes.ENOENT();
      }
      blockDevice.fsync();
      return 0;
    } catch (Throwable t) {
      LOG.error(t.getMessage(), t);
      return -ErrorCodes.EIO();
    }
  }

  @Override
  public int release(String path, FuseFileInfo fi) {
    long fileHandle = fi.fh.get();
    LOG.info("release fh {} path {}", fileHandle, path);
    try {
      if (!blockDeviceManager.close(fileHandle)) {
        return -ErrorCodes.ENOENT();
      }
      return 0;
    } catch (Throwable t) {
      LOG.error(t.getMessage(), t);
      return -ErrorCodes.EIO();
    }
  }

  @Override
  public int getattr(String path, FileStat stat) {
    LOG.info("getattr path {}", path);
    try {
      if (isRoot(path)) {
        stat.st_mode.set(FileStat.S_IFDIR);
        return 0;
      }
      String blockDeviceId = getBlockDeviceId(path);
      if (blockDeviceManager.hasBlockDevice(blockDeviceId)) {
        long length = blockDeviceManager.getLength(blockDeviceId);
        stat.st_mode.set(FileStat.S_IFREG | 0777);
        stat.st_size.set(length);
        return 0;
      }
      return -ErrorCodes.ENOENT();
    } catch (Throwable t) {
      LOG.error(t.getMessage(), t);
      return -ErrorCodes.EIO();
    }
  }

  @Override
  public int write(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
    long fileHandle = fi.fh.get();
    if (DEBUG)
      LOG.info("write fh {} path {}", fileHandle, path);
    try {
      BlockDevice blockDevice = blockDeviceManager.lookup(fileHandle);
      if (blockDevice == null) {
        return -ErrorCodes.ENOENT();
      }
      return blockDevice.write(buf, size, offset);
    } catch (Throwable t) {
      LOG.error(t.getMessage(), t);
      return -ErrorCodes.EIO();
    }
  }

  @Override
  public int read(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
    long fileHandle = fi.fh.get();
    if (DEBUG)
      LOG.info("read fh {} path {}", fileHandle, path);
    try {
      BlockDevice blockDevice = blockDeviceManager.lookup(fileHandle);
      if (blockDevice == null) {
        return -ErrorCodes.ENOENT();
      }
      return blockDevice.read(buf, size, offset);
    } catch (Throwable t) {
      LOG.error(t.getMessage(), t);
      return -ErrorCodes.EIO();
    }
  }

  @Override
  public int readdir(String path, Pointer buf, FuseFillDir filter, @off_t long offset, FuseFileInfo fi) {
    LOG.info("readdir path {}", path);
    try {
      if (isRoot(path)) {
        filter.apply(buf, DOT, null, 0);
        filter.apply(buf, DOTDOT, null, 0);
        List<String> names = blockDeviceManager.getBlockDeviceNames();
        for (String name : names) {
          filter.apply(buf, name, null, 0);
        }
        return 0;
      }
      String blockDeviceId = getBlockDeviceId(path);
      if (blockDeviceManager.hasBlockDevice(blockDeviceId)) {
        return -ErrorCodes.ENOTDIR();
      }
      return -ErrorCodes.ENOENT();
    } catch (Throwable t) {
      LOG.error(t.getMessage(), t);
      return -ErrorCodes.EIO();
    }
  }

  @Override
  public int unlink(String path) {
    return -ErrorCodes.EIO();
  }

  @Override
  public int mkdir(String path, @mode_t long mode) {
    return -ErrorCodes.EIO();
  }

  @Override
  public int rename(String path, String newName) {
    return -ErrorCodes.EIO();
  }

  @Override
  public int rmdir(String path) {
    return -ErrorCodes.EIO();
  }

  @Override
  public int truncate(String path, long offset) {
    LOG.info("truncate path {}", path);
    return -ErrorCodes.EIO();
  }

  @Override
  public int create(String path, @mode_t long mode, FuseFileInfo fi) {
    LOG.info("create path {} mode {} fh {}", path, mode, fi.fh.get());
    return -ErrorCodes.ENOENT();
  }

  private static List<String> removeEmpty(List<String> lst) {
    List<String> list = new ArrayList<>(lst);
    list.removeIf(item -> item == null || EMPTY_STRING.equals(item));
    return list;
  }

  private boolean isRoot(String path) {
    return PATH_SEP.equals(path);
  }

  private String getBlockDeviceId(String path) {
    List<String> elements = removeEmpty(PATH_SPLITTER.splitToList(path));
    if (elements.size() == 1) {
      return elements.get(0);
    }
    return null;
  }

}
