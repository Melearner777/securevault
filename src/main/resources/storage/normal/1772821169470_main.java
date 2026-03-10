
import java.util.*;
public class main {

    public static void main(String args[]){
        Scanner sc=new Scanner(System.in);
        int n=sc.nextInt();
        int t=sc.nextInt();
        int books[]=new int[n];
        for(int i=0;i<n;i++){
            books[i]=sc.nextInt();
        }
     
      
        int left=0;
        int sum=0;
        int maxBooks=0;

        for(int i=0;i<n;i++){
            sum+=books[i];

            while(sum>t){
                sum-=books[left];
                left++;
            }
            maxBooks=Math.max(maxBooks, i-left+1);
        }
        System.out.println(maxBooks);
    }
     
}