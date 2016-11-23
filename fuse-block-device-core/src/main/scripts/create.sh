#!/bin/bash

FILE_PATH=$1

# make partition
sed -e 's/\s*\([\+0-9a-zA-Z]*\).*/\1/' << EOF | fdisk ${FILE_PATH}
  o # clear the in memory partition table
  n # new partition
  p # primary partition
  1 # partition number 1
    # default - start at beginning of disk
    # default - end at end of disk
  p # print the in-memory partition table
  w # write the partition table
  q # and we're done
EOF

# print partition info
fdisk -lu ${FILE_PATH}

# get start sector
START=`fdisk -lu ${FILE_PATH} | grep ${FILE_PATH} | grep -v Disk | awk '{print $2}'`
echo "START ${START}"

# get sector size
SECTOR_SIZE=`fdisk -lu ${FILE_PATH} | grep Units | awk '{print $9}'`
echo "SECTOR_SIZE ${SECTOR_SIZE}"

# calculate offset
echo "LOOP_OFFSET=${START} * ${SECTOR_SIZE}"
LOOP_OFFSET=$((${START} * ${SECTOR_SIZE}))
echo "LOOP_OFFSET ${LOOP_OFFSET}"

# create loop device
echo "losetup -o ${LOOP_OFFSET} --show -f ${FILE_PATH}"
LOOP_DEVICE=$(losetup -o ${LOOP_OFFSET} --show -f ${FILE_PATH})
echo "LOOP_DEVICE ${LOOP_DEVICE}"

# make filesystem
echo "mkfs.ext4 ${LOOP_DEVICE}"
mkfs.ext4 ${LOOP_DEVICE}

# should pass
echo "fsck -fv ${LOOP_DEVICE}"
fsck -fv ${LOOP_DEVICE}

# release loop device
echo "losetup -d ${LOOP_DEVICE}"
losetup -d ${LOOP_DEVICE}
