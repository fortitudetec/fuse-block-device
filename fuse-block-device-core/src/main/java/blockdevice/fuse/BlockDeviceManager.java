package blockdevice.fuse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;

import com.google.common.collect.MapMaker;

public class BlockDeviceManager {

  private final Map<String, BlockDevice> blockDevs = new MapMaker().makeMap();
  private final AtomicReferenceArray<BlockDevice> fileHandles;

  public BlockDeviceManager(int maxFileHandles) {
    fileHandles = new AtomicReferenceArray<>(maxFileHandles);
  }

  public synchronized void register(BlockDevice blockDevice) throws IOException {
    if (blockDevs.containsKey(blockDevice.getId())) {
      throw new IOException("Block id [" + blockDevice.getId() + "] already registered.");
    }
    blockDevs.put(blockDevice.getId(), blockDevice);
  }

  public BlockDevice lookup(long fileHandle) {
    int id = (int) fileHandle;
    if (id < 0 || id >= fileHandles.length()) {
      return null;
    }
    return fileHandles.get(id);
  }

  public List<String> getBlockDeviceNames() {
    List<String> lst = new ArrayList<>(blockDevs.keySet());
    Collections.sort(lst);
    return lst;
  }

  public long getLength(String blockDeviceId) {
    BlockDevice blockDevice = find(blockDeviceId);
    if (blockDevice == null) {
      return -1L;
    }
    return blockDevice.length();
  }

  public boolean hasBlockDevice(String blockDeviceId) {
    return find(blockDeviceId) != null;
  }

  public BlockDevice open(String blockDeviceId) throws IOException {
    synchronized (fileHandles) {
      BlockDevice blockDevice = find(blockDeviceId);
      if (blockDevice == null) {
        return null;
      }
      int index = locateEmptyFileHandleIndex();
      if (index < 0) {
        throw new IOException("Max file handles open [" + fileHandles.length() + "].");
      }
      BlockDevice clone = blockDevice.clone();
      fileHandles.set(index, clone);
      clone.setFileHandle(index);
      clone.open();
      return clone;
    }
  }

  public boolean close(long fileHandle) throws IOException {
    synchronized (fileHandles) {
      int id = (int) fileHandle;
      if (id < 0 || id >= fileHandles.length()) {
        return false;
      }
      try (BlockDevice blockDevice = fileHandles.getAndSet(id, null)) {
        return true;
      }
    }
  }

  private int locateEmptyFileHandleIndex() {
    for (int i = 0; i < fileHandles.length(); i++) {
      if (fileHandles.get(i) == null) {
        return i;
      }
    }
    return -1;
  }

  private BlockDevice find(String blockDeviceId) {
    return blockDevs.get(blockDeviceId);
  }
}
