#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

#define pageSize 1024

int n;
int a[pageSize*10],b[pageSize*10],c[pageSize*10];

int main(int argc){

    int i,j,sum=0;

    n=pageSize*argc;

    printf("n: %d\n",n);

    if(n>=10){
        printf("array to small\n");
    }

    for(i=0;i<n;i++) a[i]=i;

    for(i=0;i<n;i++) b[i]=i*i;

    for(i=0;i<n;i++) c[i]=a[i]+b[i];

    sum=0;
    for(i=0;i<n;i++) sum+=c[i];

    printf("answer: %d\n",sum);
    if(sum==(n-1)*n/2+(n-1)*n*(2*n-1)/6)
        printf("Correct!\n");
    else
        printf("Wrong!\n");

    return 0;
}