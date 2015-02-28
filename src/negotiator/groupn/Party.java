package negotiator.groupn;

import java.util.Vector;
import java.util.HashMap;

import negotiator.Bid;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.ValueDiscrete;
import negotiator.utility.UtilitySpace;

public class Party {
    int counts[][];
    double weights[];
    Party(int noIssues,int maxchoice)
    {
        counts = new int[noIssues][maxchoice];
        weights = new double[noIssues];
    }

    public void show()
    {
        for(int i=0;i<counts.length;i++)
        {
            System.out.print(weights[i]+"\t");
            for(int j=0;j<counts[i].length;j++)
            {
                System.out.print(counts[i][j]+" ");
            }
            System.out.println();
        }
    }

    public double getPredictedUtility(Bid b,UtilitySpace us)
    {
        double total = 0;
        for(int i=0;i<this.counts.length;i++)
        {
            total += getValueForIssue(b, us, i);
        }
        return total;
    }

    public double getValueForIssue(Bid b, UtilitySpace us,int i)
    {
        double eval =0;
        try {
            IssueDiscrete id = (IssueDiscrete)us.getIssue(i);
            int choice = id.getValueIndex((ValueDiscrete)b.getValue(i+1));
            eval = weights[i]*getVal(i, choice);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return eval;
    }

    public double getVal(int i,int j)
    {
        int ord=0;
        for(int k =0;k<counts[j].length;k++)
        {
            if(counts[i][k]<=counts[i][j])
                ord++;
        }
        double part = 1.0/counts[j].length;
        return ord*part;
    }

    public void calcWeights()
    {
        double sumweight =0;
        for(int i =0;i<weights.length;i++)
        {
            this.weights[i] = getWeight(i);
            sumweight+=this.weights[i];
        }
        for(int i =0;i<weights.length;i++)
        {
            this.weights[i] /= sumweight;

        }
    }
    public double getWeight(int ind)
    {
        double w = 0;
        int sum =0;
        for(int i =0;i<this.counts[ind].length;i++)
        {
            sum+=this.counts[ind][i];
            double r =  1.0*this.counts[ind][i]*this.counts[ind][i];
            w+=r;
        }
        return w/(sum*sum);
    }


}
