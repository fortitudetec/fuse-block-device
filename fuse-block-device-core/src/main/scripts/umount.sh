#!/bin/bash

MOUNT_POINT=$(realpath $1)

LOOP_DEVICE=$(cat /proc/mounts | grep ${MOUNT_POINT} | awk '{print $1}')

# mount filesystem
echo "umount ${MOUNT_POINT}"
umount ${MOUNT_POINT}

# remove loop device
echo "losetup -d ${LOOP_DEVICE}"
losetup -d ${LOOP_DEVICE}
