#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

#define pageSize 1024

int n,m,p,l;
char a[pageSize*20];

int get_number(){
    int ret=0;

    if(p>=l){
        printf("no number leaf\n");
        return 0;
    }

    while(p<l){
        while(a[p]<48 || a[p]>57) p++;
        ret=0;
        while(a[p]>=48 && a[p]<=57) ret=ret*10+a[p++]-48;
    }

    return ret;
}

int main(char *argv1,int argv2){

    int i,j,amount,src,ans,sum1=0,sum2=0;

    src=open(argv1);

    m=argv2;

    printf("read %d pages\n",m);

    amount=read(src, a, pageSize*m);

    printf("amount: %d\n",amount);

    l=strlen(a);

    p=0;
    n=get_number();
    for(i=1;i<=n;i++) sum1+=get_number();
    for(i=1;i<=n;i++) sum2+=get_number();   

    printf("difference %d\n",sum1-sum2);

    ans=get_number();
    printf("answer %d\n",ans); 

    return 0;
}