package utils;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Locale;

public class Evaluator {
    public static class RankedMetrics {
        public double pAt10;
        public double pAt50;
        public double elevenPointAP;
        public double[] elevenPoints = new double[11];
        public double[] pAtKArray = new double[52];
        public double[] rAtKArray = new double[52];
        public long durationNs;

        public void addTo(RankedMetrics other) {
            this.pAt10 += other.pAt10;
            this.pAt50 += other.pAt50;
            this.elevenPointAP += other.elevenPointAP;
            for (int i = 0; i < 11; i++) {
                this.elevenPoints[i] += other.elevenPoints[i];
            }
            for (int i = 0; i < 52; i++) {
                this.pAtKArray[i] += other.pAtKArray[i];
                this.rAtKArray[i] += other.rAtKArray[i];
            }
            this.durationNs += other.durationNs;
        }

        public void divideBy(int count) {
            if (count > 0) {
                this.pAt10 /= count;
                this.pAt50 /= count;
                this.elevenPointAP /= count;
                for (int i = 0; i < 11; i++) {
                    this.elevenPoints[i] /= count;
                }
                for (int i = 0; i < 52; i++) {
                    this.pAtKArray[i] /= count;
                    this.rAtKArray[i] /= count;
                }
                this.durationNs /= count;
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Time: %.2f ms | P@10: %.4f | P@50: %.4f | 11-pt AP: %.4f\n",
                    durationNs / 1_000_000.0, pAt10, pAt50, elevenPointAP));
            sb.append("   [11-point Precision levels (0.0 to 1.0)]: ");
            for (int i = 0; i < 11; i++) {
                sb.append(String.format("%.4f", elevenPoints[i])).append(i < 10 ? ", " : "");
            }
            sb.append("\n   [52-point Precision Arrays]:\n    P: [");
            for (int i = 0; i < 52; i++) {
                sb.append(String.format("%.4f", pAtKArray[i])).append(i < 51 ? ", " : "");
            }
            sb.append("]\n    R: [");
            for (int i = 0; i < 52; i++) {
                sb.append(String.format("%.4f", rAtKArray[i])).append(i < 51 ? ", " : "");
            }
            sb.append("]");
            return sb.toString();
        }
    }

    public static class Metrics {
        public double precision;
        public double recall;
        public double f1;
        public double accuracy;
        public long durationNs;

        public void addTo(Metrics other) {
            this.precision += other.precision;
            this.recall += other.recall;
            this.f1 += other.f1;
            this.accuracy += other.accuracy;
            this.durationNs += other.durationNs;
        }

        public void divideBy(int count) {
            if (count > 0) {
                this.precision /= count;
                this.recall /= count;
                this.f1 /= count;
                this.accuracy /= count;
                this.durationNs /= count;
            }
        }

        @Override
        public String toString() {
            return String.format("Time: %.2f ms | Precision: %.4f | Recall: %.4f | F1: %.4f | Accuracy: %.4f",
                    durationNs / 1_000_000.0, precision, recall, f1, accuracy);
        }
    }

    public static Metrics evaluate(Set<Integer> groundTruth, Set<Integer> results, int totalDocs) {
        Metrics m = new Metrics();
        if (groundTruth == null)
            groundTruth = new HashSet<>();
        if (results == null)
            results = new HashSet<>();

        int tp = 0;
        for (Integer res : results) {
            if (groundTruth.contains(res))
                tp++;
        }

        int fp = results.size() - tp;
        int fn = groundTruth.size() - tp;
        int tn = totalDocs - tp - fp - fn;

        m.precision = (tp + fp > 0) ? (double) tp / (tp + fp) : 0.0;
        m.recall = (groundTruth.size() > 0) ? (double) tp / groundTruth.size() : 0.0;
        m.f1 = (m.precision + m.recall > 0) ? 2 * m.precision * m.recall / (m.precision + m.recall) : 0.0;
        m.accuracy = (double) (tp + tn) / totalDocs;

        return m;
    }

    public static RankedMetrics evaluateRanked(Set<Integer> groundTruth, List<Integer> rankedResults) {
        RankedMetrics m = new RankedMetrics();
        if (groundTruth == null || groundTruth.isEmpty() || rankedResults == null || rankedResults.isEmpty()) {
            return m;
        }

        int tp = 0;
        int gtSize = groundTruth.size();
        List<Double> precisions = new ArrayList<>();
        List<Double> recalls = new ArrayList<>();

        for (int i = 0; i < 52; i++) {
            if (i < rankedResults.size() && groundTruth.contains(rankedResults.get(i))) {
                tp++;
            }
            double currP = (double) tp / (i + 1);
            double currR = (double) tp / gtSize;

            if (i < rankedResults.size()) {
                precisions.add(currP);
                recalls.add(currR);
            }
            m.pAtKArray[i] = currP;
            m.rAtKArray[i] = currR;

            if (i == 9) {
                m.pAt10 = currP;
            }
            if (i == 49) {
                m.pAt50 = currP;
            }
        }

        if (rankedResults.size() < 10)
            m.pAt10 = (double) tp / 10;
        if (rankedResults.size() < 50)
            m.pAt50 = (double) tp / 50;

        double elevenPtSum = 0;
        for (int i = 0; i <= 10; i++) {
            double targetR = i / 10.0;
            double maxP = 0.0;
            for (int j = 0; j < precisions.size(); j++) {
                if (recalls.get(j) >= targetR && precisions.get(j) > maxP) {
                    maxP = precisions.get(j);
                }
            }
            m.elevenPoints[i] = maxP;
            elevenPtSum += maxP;
        }
        m.elevenPointAP = elevenPtSum / 11.0;

        return m;
    }
}
