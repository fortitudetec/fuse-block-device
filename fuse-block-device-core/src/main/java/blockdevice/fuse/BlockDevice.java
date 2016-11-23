package blockdevice.fuse;

import java.io.Closeable;
import java.io.IOException;

import jnr.ffi.Pointer;

public abstract class BlockDevice implements Closeable, Cloneable {

  private long fileHandle;

  /**
   * The instance file handle (unsigned int).
   * 
   * @return the file handle (unsigned int).
   */
  public final long getFileHandle() {
    return fileHandle;
  }

  public final void setFileHandle(long fileHandle) {
    this.fileHandle = fileHandle;
  }

  /**
   * Called when the block device is opened.
   * 
   * @throws IOException
   */
  public abstract void open() throws IOException;

  /**
   * The id is the file name show in the mount.
   * 
   * @return returns the id for this block device.
   */
  public abstract String getId();

  /**
   * The length the of block device.
   * 
   * @return the length.
   */
  public abstract long length();

  /**
   * Sync the current file handle to the underlying media.
   * 
   * @throws IOException
   */
  public abstract void fsync() throws IOException;

  /**
   * Writes data to the block device.
   * 
   * @param buf
   *          the buffer.
   * @param size
   *          the amount to write.
   * @param offset
   *          the offset into the block device.
   * @return the number of bytes written.
   * @throws IOException
   */
  public abstract int write(Pointer buf, long size, long offset) throws IOException;

  /**
   * Reads data from the block device.
   * 
   * @param buf
   *          the buffer.
   * @param size
   *          the amount to write.
   * @param offset
   *          the offset into the block device.
   * @return the number of bytes written.
   * @throws IOException
   */
  public abstract int read(Pointer buf, long size, long offset) throws IOException;

  @Override
  public BlockDevice clone() {
    try {
      return (BlockDevice) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException("Won't ever happen");
    }
  }

}
