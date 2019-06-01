#include <bits/stdc++.h>

#define TX0 30000003

using namespace std;

int main() {
    ifstream txin("./txtime.txt");
    ofstream out("./txdata.txt");
    txin.tie(NULL); out.tie(NULL);

    vector<long long> timestamps;

    long long txid, t;
    while (txin >> txid >> t && txid<=TX0+10000) {
        if (txid >= TX0)
            timestamps.push_back(t);
    }

    sort(timestamps.begin(),timestamps.end());
    int last=0;
    while (timestamps[1]-timestamps[0] > 5 && !timestamps.empty()) {
        timestamps.erase(timestamps.begin());
    }
    while (timestamps[last+1]-timestamps[last] < 15 && last < timestamps.size()) {
        last++;
    }
    for (int i=0; i<last; i++) { 
        out << timestamps[i]-timestamps[0] << "\n";
    }
    return 0;
}