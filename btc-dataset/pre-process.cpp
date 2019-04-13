#include <bits/stdc++.h>

#define TX0 30000000

using namespace std;

long long ct = 0;

bool empty_intersection(const set<long long>& x, const set<long long>& y) {
    auto i = x.begin();
    auto j = y.begin();
    while (i != x.end() && j != y.end()) {
        if (*i == *j) return false;
        else if (*i < *j) ++i;
        else ++j;
    }
    return true;
}

void set_union(set<long long> &s1, const set<long long> &s2) {
    for (auto x : s2) s1.emplace(x);
}

vector<long long> get_addr(vector<set<long long> > &v) {
    int n = v.size();
    ct = 0;
    vector<long long> addr(n,-1);
    vector<set<long long>*> sets(n,NULL);
    for (int i=0; i<n; i++) {
        sets[i] = &(v[i]);
    }
    for (int i=0; i<n; i++) {
        if (addr[i] < 0) {
            addr[i] = ct++;
        }
        for (int j=i+1; j<n; j++) {
            if (!empty_intersection(*(sets[i]),*(sets[j]))) {
                addr[j] = addr[i];
                set_union(*(sets[i]),*(sets[j]));
                sets[j] = sets[i];
            }
        }
    }
    return addr;
}

int main() {
    ifstream inTx("./txin.txt");
    ifstream txTime("./txtime.txt");
    ofstream out("./txdata.txt");

    inTx.tie(NULL); txTime.tie(NULL); out.tie(NULL);

    vector<long long> timestamps;
    vector<set<long long> > addresses;

    long long txid, t;
    while (txTime >> txid >> t && txid<=TX0+10000) {
        timestamps.push_back(t);
    }

    cout << timestamps.size() << endl;

    addresses.resize(timestamps.size());
    long long addr, val;
    while (inTx >> txid >> addr >> val && txid<=TX0+10000) {
        addresses[txid-TX0].emplace(addr);
    }

    cout << addresses.size() << endl;

    vector<long long> mapped = get_addr(addresses);
    cout << ct << " distinct addresses found\n";
    out << ct << endl;
    int skipped = 0;
    for (int i=0; i<timestamps.size(); i++) {
        if (addresses[i].size() > 0)
            out << timestamps[i]-timestamps[0] << "\t" << mapped[i] << "\n";
        else skipped++;
    }
    cout << "Skipped " << skipped << endl;
    return 0;
}