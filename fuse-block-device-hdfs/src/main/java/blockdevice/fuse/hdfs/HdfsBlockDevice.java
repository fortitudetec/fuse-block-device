package blockdevice.fuse.hdfs;

import java.io.IOException;

import blockdevice.fuse.BlockDevice;
import jnr.ffi.Pointer;

public class HdfsBlockDevice extends BlockDevice {

  @Override
  public void close() throws IOException {
    // TODO Auto-generated method stub

  }

  @Override
  public void open() throws IOException {
    // TODO Auto-generated method stub

  }

  @Override
  public String getId() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public long length() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void fsync() throws IOException {
    // TODO Auto-generated method stub

  }

  @Override
  public int write(Pointer buf, long size, long offset) throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int read(Pointer buf, long size, long offset) throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

}
