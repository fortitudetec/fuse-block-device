package blockdevice.fuse.memory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blockdevice.fuse.BlockDevice;
import blockdevice.fuse.BlockDeviceFuseFS;
import blockdevice.fuse.BlockDeviceManager;
import jnr.ffi.Pointer;

public class MemoryBlockDeviceFuseFS {
  private static final Logger LOG = LoggerFactory.getLogger(MemoryBlockDeviceFuseFS.class);

  public static void main(String[] args) throws IOException {

    BlockDeviceManager blockDeviceManager = new BlockDeviceManager(16 * 1024);
    blockDeviceManager.register(MemoryBlockDevice.create("aa", ByteBuffer.allocate(1024 * 1024 * 100)));
    blockDeviceManager.register(MemoryBlockDevice.create("bb", ByteBuffer.allocate(1024 * 1024 * 100)));

    BlockDeviceFuseFS fs = new BlockDeviceFuseFS(blockDeviceManager);
    try {
      Path path = Paths.get(args[0]).toAbsolutePath();
      LOG.info("Mounting fs @ {}", path);
      fs.mount(path, true);
    } finally {
      fs.umount();
    }
  }

  public static class MemoryBlockDevice extends BlockDevice {

    private final long length;
    private final ByteBuffer contents;
    private final String id;

    public MemoryBlockDevice(String id, ByteBuffer contents) {
      this.id = id;
      this.contents = contents;
      this.length = contents.capacity();
    }

    public static MemoryBlockDevice create(String id, ByteBuffer contents) {
      return new MemoryBlockDevice(id, contents);
    }

    @Override
    public int write(Pointer buf, long size, long offset) throws IOException {
      ByteBuffer duplicate = contents.duplicate();
      int len = (int) size;
      byte[] bytesToWrite = new byte[len];
      buf.get(0, bytesToWrite, 0, len);
      duplicate.position((int) offset);
      duplicate.put(bytesToWrite);
      return len;
    }

    @Override
    public int read(Pointer buf, long size, long offset) throws IOException {
      ByteBuffer duplicate = contents.duplicate();
      int bytesToRead = (int) Math.min(length - offset, size);
      byte[] bytesRead = new byte[bytesToRead];
      duplicate.position((int) offset);
      duplicate.get(bytesRead, 0, bytesToRead);
      buf.put(0, bytesRead, 0, bytesToRead);
      return bytesToRead;
    }

    @Override
    public void open() throws IOException {

    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public long length() {
      return length;
    }

    @Override
    public void fsync() throws IOException {

    }
  }
}
