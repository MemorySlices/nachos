#include<cstdio>
#include<iostream>
#include<cstdlib>
#include<ctime>
using namespace std;

int n;
int a[100010],b[100010];

int main(){
    srand(time(NULL));

    freopen("test2","w",stdout);

    int i,sum=0,sum1=0,sum2=0;

    scanf("%d",&n);

    for(i=1;i<=n;i++){
        a[i]=rand()%1000;
        b[i]=rand()%1000;
    }

    printf("%d\n",n);

    for(i=1;i<=n;i++){
        printf("%d ",a[i]);
        sum1+=a[i];
    }
    printf("\n");

    for(i=1;i<=n;i++){
        printf("%d ",b[i]);
        sum2+=b[i];
    }
    printf("\n");

    printf("%d\n",sum1-sum2);

    return 0;
}
