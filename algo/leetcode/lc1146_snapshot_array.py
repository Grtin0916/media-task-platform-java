#!/usr/bin/env python3
"""LeetCode 1146 Snapshot Array: set/snap O(1), get O(log k)."""
from bisect import bisect_right
class SnapshotArray:
    def __init__(self,length):self.snap_id=0;self.values=[[(0,0)] for _ in range(length)]
    def set(self,index,val):
        if self.values[index][-1][0]==self.snap_id:self.values[index][-1]=(self.snap_id,val)
        else:self.values[index].append((self.snap_id,val))
    def snap(self):current=self.snap_id;self.snap_id+=1;return current
    def get(self,index,snap_id):
        history=self.values[index];return history[bisect_right(history,(snap_id,float("inf")))-1][1]
if __name__=="__main__":
    a=SnapshotArray(3);assert a.get(0,0)==0
    a.set(0,5);s0=a.snap();assert a.get(0,s0)==5
    a.set(0,6);a.set(0,7);s1=a.snap();assert a.get(0,s1)==7 and a.get(0,s0)==5
    assert a.get(1,s1)==0
    a.set(2,9);s2=a.snap();assert a.get(2,s1)==0 and a.get(2,s2)==9
    for i in range(100):a.set(1,i)
    s3=a.snap();assert a.get(1,s3)==99
    a.set(2,11);assert a.get(2,s2)==9
    b=SnapshotArray(1);b.set(0,1);assert b.snap()==0 and b.get(0,0)==1
    print("LC1146 tests passed: 8/8")
