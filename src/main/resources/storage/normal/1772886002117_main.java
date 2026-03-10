

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Queue;
import java.util.*;
import java.util.StringTokenizer;

public class main {

    static class Pair{
        int row;
        int col;
        Pair(int row, int col){
            this.row=row;
            this.col=col;
        }
    }
    static int rowD[]={1,-1,0,0};
    static int colD[]={0,0,-1,1};
    static char dir[]={'D','U','L','R'};

    

    public static void main(String[] args) throws Exception{
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in)) ;
        StringTokenizer st = new StringTokenizer(br.readLine());
        
        int n = Integer.parseInt(st.nextToken());
        int m = Integer.parseInt(st.nextToken());
        boolean vis[][]=new boolean[n][m];
        char[][] grid = new char[n][m];
        

        int startRow = 0, startCol = 0;
        int endRow = 0, endCol = 0;

        for (int i = 0; i < n; i++) {
            String line = br.readLine();
            for (int j = 0; j < m; j++) {
                grid[i][j] = line.charAt(j);

                if (grid[i][j] == 'A') {
                    startRow = i;
                    startCol = j;
                }
                if (grid[i][j] == 'B') {
                    endRow = i;
                    endCol = j;
                }
            }
        }


        Queue<Pair> q=new LinkedList<>();
        q.add(new Pair(startRow,startCol));
        vis[startRow][startCol]=true;
        int [][] parentRow=new int[n][m];
        int [][] parentCol=new int[n][m];
        char[][] movedir=new char[n][m];

        boolean found=false;
        while(!q.isEmpty()){
            Pair curr=q.poll();

            if(grid[curr.row][curr.col]=='B'){
                found=true;
                break;
            }
            for(int i=0;i<4;i++){
                int newRow=curr.row+rowD[i];
                int newCol=curr.col+colD[i];

                if(newRow>=0 && newRow<n && newCol>=0 && newCol<m && !vis[newRow][newCol] && (grid[newRow][newCol]=='.' ||  grid[newRow][newCol]=='B' )){
                    parentRow[newRow][newCol]=curr.row;
                    parentCol[newRow][newCol]=curr.col;
                    movedir[newRow][newCol]=dir[i];
                    vis[newRow][newCol]=true;
                    q.add(new Pair(newRow, newCol));
                }
            }
        }
        if(!found){
            System.out.println("NO");
            return;
        }
        StringBuilder path=new StringBuilder();
        int r=endRow;
        int c=endCol;  

        while (r!=startRow || c!=startCol) {
            path.append(movedir[r][c]);
            int pr=parentRow[r][c];
            int pc=parentCol[r][c];
            r=pr;
            c=pc;
            
            
        }
        path.reverse();

        System.out.println("YES");
        System.out.println(path.length());
        System.out.println(path);
    

    }
}
