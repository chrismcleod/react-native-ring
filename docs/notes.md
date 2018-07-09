# Algorithm for replicating data in Android

The algorithm below describes the process for replicating data to
the isolated android data folder across separate apps.  This algorithm
is NOT proven correct nor ACID. It does NOT guarantee: there will be no data loss,
data corruption, and that availability of nodes is always 100%.  (i.e. the app could
get in a state where any attempt to use this module in native code will crash the app
even if the code is in another app).

React Native Ring: Simple Android Decentralized Replication (SADR):
------------------------------------------------

NODE INITIALIZATION
- Node is constructed with a domain name
- Node locally elects leader by finding the first installed package matching the domain when sorted by installation date
- If node IS NOT leader:
  - Node performs a read on the leader to intialize/synchronize it's data
  - If versions match, respond with current state
  - If node version is smaller than leader, node updates its state to match leader and responds with that state
  - Node state will never be greater than leader here, because upon reading, a leader would have queried all nodes for the most recent version if neccessary
- If node IS leader:
  - Read and respond with current state

NODE READ
- If node IS NOT leader:
  - Fail if node has not been initialized
  - Respond with current state
- If node IS leader:
  - If this node has no state, initialize to default new state (also set never been leader to true)
  - Check state to determine if this node has ever been initialized as the leader
    - If not, inform all other nodes of this node as new leader, wait for their responses with their current state (take care to prevent deadlock here, i.e. if this read was invoked by a follower, the follower would be waiting on the master and the master would be waiting on the follower)
    - If any node fails to respond to this request, the read will fail
    - If any node has state greater than this leader node, update leader node to the highest state
    - Save state indicating this master has been initialized as the master
  - Respond with current state

NODE WRITE
- Fail if node has not been initialized
- If node IS NOT leader:
  - Forward write to leader node
  - Respond with leader response
- If node IS leader:
  - If locked
    - clear lock if it has timed out and broadcast current state to all nodes
    - else fail
  - If version does not match
    - Broadcast update to node that initiated request
    - Fail
  - Else, lock
  - Commit update
  - Broadcast update to all nodes
  - Unlock
  - Respond with current state

NODE NEW LEADER ELECTED
- Clear state indicating this node has been initialized as the master if it exists
- Respond with current state (or default state if never initialized)


Failure Scenarios:
---------

- Write succeeds at leader, but fails to respond to Node
  - Node (and JS) will have previous version on the next write attempt

- Write succeeds at leader, succeeds at Node, but fails to respond to JS
  - JS will have previous version on the next write attempt

- Write succeeds at leader, succeeds at some Nodes, Leader is removed and new Leader is elected that does not have most recent version
  - Node will have a version greater than leader node

- Leader begins transaction, locks itself, then fails
  - On future requests, leader will be locked

- Leader begins transaction, locks itself, writes data, then fails
  - On future requests, leader will be locked and cohorts will not have most recent version


Failure States:
1.  Node (and JS) have previous version compared to leader
    a.  Leader propagates version failure to Node (with most recent data)
    b.  Node saves most recent data
    c.  Node propagates failure to JS with most recent data
    d.  JS updates data
    e.  JS propagates error to client
2.  JS has version less than own Node
    a.  Node propagates failure to JS with most recent data
    b.  JS updates data
    c.  JS propagates error to client
3.  Node has version greater than leader
    a.  A leader node will have state indicating whether it has ever processed a request as the leader
    b.  If it has never been leader, it will inform all other nodes that it is now the leader
    c.  All other nodes will clear their state indicating if they have processed a request as leader
    d.  Other nodes will respond with their current version and data
    e.  If the new leader discovers a node with a larger version than it is aware of, it sets it's state to that state and aborts the current request
    f.  If any node fails to respond to this request, leader fails without saving state indicating it has been leader
6.  Leader is locked with no running transaction, nodes are in sync
    a.  Leader will continue to fail for all requests until the lock times out
7.  Leader is locked with no running transaction, nodes are lagging behind
    a.  Leader will continue to fail for all requests until the lock times out
    b.  If a leader node lock has timed out, broadcast current leader state to all nodes and abort current request

