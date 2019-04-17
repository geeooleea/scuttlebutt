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
    for (int i=2; i<timestamps.size(); i++) {
        out << timestamps[i]-timestamps[2] << "\n";
    }
    return 0;
}