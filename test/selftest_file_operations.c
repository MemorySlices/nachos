#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

#define BUFSIZE 1024

char buf[BUFSIZE];

void cp(char*argv1, char*argv2){
  int src, dst, amount;
  
  src = open(argv1);
  if (src==-1) {
    printf("Unable to open %s\n", argv1);
    return;
  }

  printf("create %d\n", creat(argv2));
  dst = open(argv2);
  if (dst==-1) {
    printf("Unable to create %s\n", argv2);
    return;
  }
  
  printf("src=%d dst=%d\n",src,dst);

  while ((amount = read(src, buf, BUFSIZE))>0) {
    write(dst, buf, amount);
  }

  close(src);
  close(dst);
}

void mv(char*argv1, char*argv2){
  int src, dst, amount;

  src = open(argv1);
  if (src==-1) {
    printf("Open to open %s\n", argv1);
    return;
  }

  printf("create %d\n", creat(argv2));
  dst = open(argv2);
  if (dst==-1) {
    printf("Unable to create %s\n", argv2);
    return;
  }
  
  printf("src=%d dst=%d\n",src,dst);

  while ((amount = read(src, buf, BUFSIZE))>0) {
    write(dst, buf, amount);
  }

  close(src);
  close(dst);
  unlink(argv1);
}

// test for all file system calls
void test1(){
  buf[0]='t';buf[1]='e';buf[2]='s';buf[3]='t';buf[4]='\n';buf[5]=0;
  int p=creat("testfile1");
  write(p,buf,6);
  close(p);
  cp("testfile1","testfile2"); // exist 1,2
  mv("testfile1","testfile3"); // exist 2,3
  cp("testfile2","testfile4"); // exist 2,3,4
  cp("testfile3","testfile5"); // exist 2,3,4,5
  mv("testfile2","testfile1"); // exist 1,3,4,5
  cp("testfile1","testfile6"); // exist 1,3,4,5,6
  mv("testfile1","testfile2"); // exist 2,3,4,5,6
  // files 2~6 contain "test"
}

// test for unlink()
void test2(){
  // unlink before open
  int p=creat("testfile1");
  buf[0]='t';buf[1]='e';buf[2]='s';buf[3]='t';buf[4]='\n';buf[5]=0;
  write(p,buf,6);
  close(p);
  unlink("testfile1");
  printf("open %d\n",p=open("testfile1")); // -1
  printf("read %d\n",read(p,buf,8)); // -1
  printf("close %d\n",close(p)); // -1
  
  // unlink after open
  p=creat("testfile1");
  buf[0]='t';buf[1]='e';buf[2]='s';buf[3]='t';buf[4]='\n';buf[5]=0;
  write(p,buf,6);
  close(p);
  int q=open("testfile1");
  unlink("testfile1");
  printf("open %d\n",q); // 2
  printf("open %d\n",open("testfile1")); // -1
  printf("read %d\n",read(q,buf,8)); // 6
  printf("%s",buf); // "test"
  printf("close %d\n",close(q)); // 0
  
  // communicate after unlink
  p=creat("testfile1"); // 2
  buf[0]='t';buf[1]='e';buf[2]='s';buf[3]='t';buf[4]='\n';buf[5]=0;
  // send 3 "test"s
  write(p,buf,5);
  write(p,buf,5);
  write(p,buf,6);
  q=open("testfile1");
  unlink("testfile1");
  printf("open %d\n",q); // 3
  printf("read %d\n",read(q,buf,64)); // 16
  printf("%s",buf); // receive 3 "test"s
  buf[0]='r';
  buf[5]=0;
  write(p,buf,6); // send "rest"
  buf[0]=buf[1]=buf[2]=buf[3]=buf[4]=0;
  printf("read %d\n",read(q,buf,64)); // 6
  printf("%s",buf); // receive "rest"
  // conversely send 3 "rest"s
  write(q,buf,4);
  write(q,buf,4);
  write(q,buf,6);
  printf("read %d\n",read(p,buf,64)); // 14
  printf("%s",buf); // receive "restrestrest"
  printf("close %d\n",close(q)); // 0
  close(p);
  close(q);
  
  // duplicate files
  p=creat("testfile1");
  write(p,buf+8,6);
  close(p);
  p=open("testfile1"); // 2
  unlink("testfile1");
  q=creat("testfile1");
  write(q,buf,14);
  close(q);
  q=open("testfile1"); // 3
  unlink("testfile1");
  printf("read %d %d\n",p,read(p,buf,64)); // 2 6
  printf("read %d %d\n",q,read(q,buf,64)); // 3 14
  printf("%s",buf); // restrestrest
  close(p);
  close(q);
  
  // duplicate files (for contrast)
  p=creat("testfile1");
  write(p,buf+8,6);
  close(p);
  p=open("testfile1"); // 2
  q=creat("testfile1");
  write(q,buf,14);
  close(q);
  q=open("testfile1"); // 3
  unlink("testfile1");
  printf("read %d %d\n",p,read(p,buf,64)); // 2 14
  printf("read %d %d\n",q,read(q,buf,64)); // 3 14
  close(p);
  close(q);
}

// test for stdin/stdout
void test3(){
  buf[0]='t';buf[1]='e';buf[2]='s';buf[3]='t';buf[4]='\n';buf[5]=0;
  int res0=write(0,buf,6),res1=write(1,buf,6);
  buf[0]=buf[1]=buf[2]=buf[3]=buf[4]=0;
  printf("write %d, %d\n",res0,res1); // -1, 6
  
  // read 8 chars
  int i=0,s=0;//while(i<10000000)s=(s+i++)%19930531;printf("%d\n",s); // wait
  res1=read(1,buf,8);
  res0=read(0,buf,8);
  printf("read %d, %d\n",res0,res1); // 8, 0
  for(i=0;i<8;i++)putchar(buf[i]);puts("");
}

// test for errors
void test4(){
  // too much files
  int p=creat("testfile1"),i=0;
  for(;i<16;i++)printf("%d ",open("testfile1")); // 3 ... 15 -1 -1 -1
  printf("\n");
  for(i=2;i<16;i++)if(i!=p)close(i);
  
  // write: wrong descriptor
  printf("%d %d %d %d\n",write(15,buf,0),write(16,buf,0),write(-2147483647,buf,0),write(2147483647,buf,0)); // -1 -1 -1 -1
  // write: wrong buffer, count = 0
  printf("%d %d %d\n",write(p,(void*)0,0),write(p,(void*)-2147483647,0),write(p,(void*)2147483647,0)); // 0 -1 -1
  // write: wrong buffer, count = 1
  printf("%d %d %d\n",write(p,(void*)0,1),write(p,(void*)-1048576,1),write(p,(void*)1048576,0)); // 1 -1 -1
  // write: wrong count
  printf("%d %d %d\n",write(p,buf,0),write(p,buf,-1),write(p,buf,2147483647)); // 0 -1 -1
  
  close(p);p=open("testfile1");
  // read: wrong descriptor
  printf("%d %d %d %d\n",read(15,buf,0),read(16,buf,0),read(-2147483647,buf,0),read(2147483647,buf,0)); // -1 -1 -1 -1
  // read: wrong buffer, count = 0
  printf("%d %d %d\n",read(p,(void*)0,0),read(p,(void*)-2147483647,0),read(p,(void*)2147483647,0)); // 0 -1 -1
  // read: wrong buffer, count = 1
  printf("%d %d %d\n",read(p,(void*)0,1),read(p,(void*)-1048576,0),read(p,(void*)1048576,0)); // -1 -1 -1
  // read: wrong count
  printf("%d %d\n",read(p,buf,-1),read(p,buf,2147483647)); // -1 -1
  
  // wrong filename
  for(i=0;i<1000;i++)buf[i]='a';
  buf[1000]=0;
  printf("%d %d %d\n",creat(0),creat(""),creat(buf)); // -1, -1, -1
  printf("%d %d %d\n",open(0),open(""),open(buf)); // -1, -1, -1
  printf("%d %d %d\n",unlink(0),unlink(""),unlink(buf)); // -1, -1, -1
  
  // use undefined memory
  int q=creat("testfile2");
  buf[0]='t';buf[1]='e';buf[2]='s';buf[3]='t';buf[4]='\n';buf[5]=0;
  write(q,buf,6);
  close(q);
  q=open("testfile2");
  printf("read %d\n",read(q,0,6)); // strange position 0, read -1
  close(q);
  
  close(p);
}

int main(int argc, char** argv)
{
  test4();
/*
  int p=creat("wxj");
  int i=0,s=0;while(i<5000000)s=(s+i++)%19930531;
  printf("%d\n",s);
  unlink("wxj");
  i=0;
  while(i<5000000)s=(s+i++)%19930531;
  printf("%d\n",s);
  buf[0]='w';
  buf[1]='x';
  buf[2]='j';
  buf[3]=0;
  printf("write %d\n",write(p,buf,4));
  i=0;
  while(i<5000000)s=(s+i++)%19930531;
  printf("%d\n",s);
  close(p);*/
  //int p=open("testfile2"),q=open("testfile2");
  //printf("%d %d\n",p,q);
  buf[0]='w';
  buf[1]='x';
  buf[2]='j';
  buf[3]='\n';
  buf[4]=0;
  int p1,p2;
  /*
  p=open("testfile1");printf("%d %d\n",p,write(p,buf,4));close(p);
  p=open("testfile2");
  
//  int i=0,s=0;while(i<10000000)s=(s+i++)%19930531;
//  printf("%d\n",s);
  
  printf("%d %d\n",p,write(p,2147483647,4));close(p);
  p=open("testfile3");printf("%d %d\n",p,write(p,buf,4));close(p);*/
  
  /*
  printf("open1 %d\n",p1=creat("./testfile1"));
  printf("open2 %d\n",p2=creat("./testfile1"));
  printf("write %d\n",write(p1,buf,5));
  printf("write %d\n",write(p2,buf,5));
  int i=0,s=0;while(i<500000000)s=(s+i++)%19930531;
  printf("%d\n",s);
  unlink("./testfile1");
  printf("write %d\n",write(p1,buf,5));
  printf("write %d\n",write(p2,buf,5));
  i=0;while(i<500000000)s=(s+i++)%19930531;
  printf("%d\n",s);
  close(p1);
  close(p2);
  */
  
  
}
