#!/bin/bash

FILE_PATH=$1
MOUNT_POINT=$2

# print partition info
fdisk -lu ${FILE_PATH}

# get start sector
START=`fdisk -lu ${FILE_PATH} | grep ${FILE_PATH} | grep -v Disk | awk '{print $2}'`
echo "START ${START}"

# get sector size
SECTOR_SIZE=`fdisk -lu ${FILE_PATH} | grep Units | awk '{print $9}'`
echo "SECTOR_SIZE ${SECTOR_SIZE}"

# calculate offset
echo "LOOP_OFFSET=$((${START} + ${SECTOR_SIZE}))"
LOOP_OFFSET=$((${START} + ${SECTOR_SIZE}))
echo "LOOP_OFFSET ${LOOP_OFFSET}"

# create loop device
echo "losetup -o ${LOOP_OFFSET} --show -f ${FILE_PATH}"
LOOP_DEVICE=$(losetup -o ${LOOP_OFFSET} --show -f ${FILE_PATH})
echo "LOOP_DEVICE ${LOOP_DEVICE}"

# mount filesystem
echo "mount ${LOOP_DEVICE} ${MOUNT_POINT}"
mount ${LOOP_DEVICE} ${MOUNT_POINT}
