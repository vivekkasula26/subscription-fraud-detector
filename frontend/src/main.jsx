import React from 'react';
import ReactDOM from 'react-dom/client';
import './styles.css';

function App() {
  const cases = [
    { account: 'AC-1041', score: 82, band: 'HIGH', reason: 'Card reuse + promo stacking' },
    { account: 'AC-2187', score: 57, band: 'MEDIUM', reason: 'Device reuse' },
    { account: 'AC-3210', score: 22, band: 'LOW', reason: 'No abuse signals' }
  ];

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Ops Console</p>
          <h1>Subscription Fraud Dashboard</h1>
        </div>
        <button className="primary-button">+ New review</button>
      </header>

      <section className="summary-grid">
        <div className="summary-card">
          <span>Open cases</span>
          <strong>128</strong>
        </div>
        <div className="summary-card warning">
          <span>Auto-frozen</span>
          <strong>17</strong>
        </div>
        <div className="summary-card success">
          <span>Resolved today</span>
          <strong>42</strong>
        </div>
      </section>

      <section className="table-panel">
        <h2>Case queue</h2>
        <table>
          <thead>
            <tr>
              <th>Account</th>
              <th>Score</th>
              <th>Band</th>
              <th>Reason</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {cases.map((item) => (
              <tr key={item.account}>
                <td>{item.account}</td>
                <td>{item.score}</td>
                <td><span className={`band ${item.band.toLowerCase()}`}>{item.band}</span></td>
                <td>{item.reason}</td>
                <td>Open</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
