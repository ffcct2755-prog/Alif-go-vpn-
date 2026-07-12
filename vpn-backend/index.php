<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Alif Go VPN - Server Backend Control Panel</title>
    <!-- Tailwind CSS CDN -->
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css" rel="stylesheet">
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;700&display=swap');
        body {
            font-family: 'Space Grotesk', sans-serif;
            background-color: #0d0f12;
            color: #f3f4f6;
        }
        .glow-button {
            box-shadow: 0 0 15px rgba(16, 185, 129, 0.4);
            transition: all 0.3s ease;
        }
        .glow-button:hover {
            box-shadow: 0 0 25px rgba(16, 185, 129, 0.7);
        }
    </style>
</head>
<body class="min-h-screen flex flex-col">

    <!-- TOP HEADER BAR -->
    <header class="bg-[#141820] border-b border-[#242b35] py-4 px-6 flex items-center justify-between shadow-md">
        <div class="flex items-center space-x-3">
            <div class="bg-emerald-500 text-black px-3 py-1.5 rounded-lg font-extrabold text-lg tracking-wider">
                ALIF GO
            </div>
            <span class="text-white font-semibold text-lg">VPN Backend Panel</span>
        </div>
        <div class="flex items-center space-x-4">
            <span class="text-xs text-emerald-400 bg-emerald-950 border border-emerald-800 px-3 py-1 rounded-full flex items-center gap-1.5">
                <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                API Connected
            </span>
            <button id="logoutBtn" class="text-gray-400 hover:text-rose-400 text-sm flex items-center gap-1">
                <i class="fa-solid fa-right-from-bracket"></i> Logout
            </button>
        </div>
    </header>

    <!-- MAIN BODY SECTION -->
    <main class="flex-1 max-w-7xl w-full mx-auto p-6 grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        <!-- LEFT PANEL: ADD SERVER FORM -->
        <div class="lg:col-span-1 bg-[#141820] border border-[#242b35] p-5 rounded-2xl flex flex-col space-y-4 shadow-xl">
            <h2 class="text-lg font-bold text-white flex items-center gap-2 border-b border-[#242b35] pb-3">
                <i class="fa-solid fa-server text-emerald-400"></i> Publish New Server
            </h2>
            
            <form id="addServerForm" class="flex flex-col space-y-3">
                <div>
                    <label class="block text-xs font-semibold text-gray-400 mb-1">Country Name *</label>
                    <input type="text" id="countryName" required placeholder="e.g. Singapore" 
                           class="w-full bg-[#1c222b] border border-[#2d3644] text-white px-3 py-2 rounded-lg text-sm focus:outline-none focus:border-emerald-500">
                </div>
                
                <div class="grid grid-cols-2 gap-3">
                    <div>
                        <label class="block text-xs font-semibold text-gray-400 mb-1">Country Code *</label>
                        <input type="text" id="countryCode" required placeholder="e.g. SG" maxlength="3"
                               class="w-full bg-[#1c222b] border border-[#2d3644] text-white px-3 py-2 rounded-lg text-sm focus:outline-none focus:border-emerald-500 uppercase">
                    </div>
                    <div>
                        <label class="block text-xs font-semibold text-gray-400 mb-1">City Location</label>
                        <input type="text" id="city" placeholder="e.g. Jurong" 
                               class="w-full bg-[#1c222b] border border-[#2d3644] text-white px-3 py-2 rounded-lg text-sm focus:outline-none focus:border-emerald-500">
                    </div>
                </div>

                <div>
                    <label class="block text-xs font-semibold text-gray-400 mb-1">IP Address / Hostname *</label>
                    <input type="text" id="ipAddress" required placeholder="e.g. 128.199.112.5" 
                           class="w-full bg-[#1c222b] border border-[#2d3644] text-white px-3 py-2 rounded-lg text-sm focus:outline-none focus:border-emerald-500">
                </div>

                <div class="grid grid-cols-2 gap-3">
                    <div>
                        <label class="block text-xs font-semibold text-gray-400 mb-1">Server Category</label>
                        <select id="type" class="w-full bg-[#1c222b] border border-[#2d3644] text-white px-3 py-2 rounded-lg text-sm focus:outline-none focus:border-emerald-500">
                            <option value="Free">Free Tier</option>
                            <option value="Premium">Premium Tier</option>
                            <option value="Gaming">Gaming Optimized</option>
                            <option value="Streaming">Streaming Optimized</option>
                        </select>
                    </div>
                    <div>
                        <label class="block text-xs font-semibold text-gray-400 mb-1">Connection Protocol</label>
                        <select id="protocol" class="w-full bg-[#1c222b] border border-[#2d3644] text-white px-3 py-2 rounded-lg text-sm focus:outline-none focus:border-emerald-500">
                            <option value="WireGuard">WireGuard</option>
                            <option value="OpenVPN">OpenVPN</option>
                            <option value="IKEv2">IKEv2</option>
                        </select>
                    </div>
                </div>

                <div class="grid grid-cols-2 gap-3 pb-2">
                    <div>
                        <label class="block text-xs font-semibold text-gray-400 mb-1">Simulated Ping (ms)</label>
                        <input type="number" id="latency" placeholder="e.g. 25" min="1" max="999"
                               class="w-full bg-[#1c222b] border border-[#2d3644] text-white px-3 py-2 rounded-lg text-sm focus:outline-none focus:border-emerald-500">
                    </div>
                    <div>
                        <label class="block text-xs font-semibold text-gray-400 mb-1">Simulated Load (%)</label>
                        <input type="number" id="loadPercent" placeholder="e.g. 15" min="0" max="100"
                               class="w-full bg-[#1c222b] border border-[#2d3644] text-white px-3 py-2 rounded-lg text-sm focus:outline-none focus:border-emerald-500">
                    </div>
                </div>

                <button type="submit" class="w-full bg-emerald-500 hover:bg-emerald-600 text-black py-2.5 px-4 rounded-xl font-bold text-sm tracking-wide glow-button">
                    <i class="fa-solid fa-paper-plane mr-1.5"></i> Publish Server Live
                </button>
            </form>
            
            <!-- OPTIONAL OVPN IMPORT FOR WEB -->
            <div class="mt-4 pt-4 border-t border-[#242b35]">
                <h3 class="text-sm font-bold text-gray-300 mb-2 flex items-center gap-2">
                    <i class="fa-solid fa-file-invoice text-emerald-400"></i> Import .ovpn directly
                </h3>
                <input type="file" id="ovpnFile" accept=".ovpn,.conf" class="hidden">
                <button onclick="document.getElementById('ovpnFile').click()" class="w-full border border-dashed border-[#3a4555] bg-[#1c222b] text-gray-300 py-3 rounded-xl hover:border-emerald-500 text-xs text-center cursor-pointer">
                    <i class="fa-solid fa-cloud-arrow-up mr-2 text-emerald-400"></i> Choose OVPN Config File
                </button>
            </div>
        </div>

        <!-- RIGHT PANEL: SERVER RELAY LIST -->
        <div class="lg:col-span-2 bg-[#141820] border border-[#242b35] p-5 rounded-2xl flex flex-col shadow-xl">
            <div class="flex items-center justify-between border-b border-[#242b35] pb-3 mb-4">
                <h2 class="text-lg font-bold text-white flex items-center gap-2">
                    <i class="fa-solid fa-list-check text-emerald-400"></i> Active Server Relays
                </h2>
                <button onclick="loadServers()" class="text-xs bg-[#1c222b] border border-[#2d3644] text-gray-300 hover:text-emerald-400 px-3 py-1.5 rounded-lg flex items-center gap-1.5">
                    <i class="fa-solid fa-arrows-rotate animate-spin-slow"></i> Refresh
                </button>
            </div>

            <!-- SERVER LIST CONTAINER -->
            <div class="flex-1 overflow-x-auto">
                <table class="w-full text-left text-sm">
                    <thead>
                        <tr class="text-gray-400 border-b border-[#242b35] text-xs">
                            <th class="pb-3 pr-3 font-semibold">Location</th>
                            <th class="pb-3 px-3 font-semibold">IP Address</th>
                            <th class="pb-3 px-3 font-semibold">Tier</th>
                            <th class="pb-3 px-3 font-semibold">Method</th>
                            <th class="pb-3 pl-3 font-semibold text-right">Actions</th>
                        </tr>
                    </thead>
                    <tbody id="serverTableBody" class="divide-y divide-[#242b35]">
                        <!-- Rendered by JS -->
                        <tr>
                            <td colspan="5" class="py-12 text-center text-gray-500">
                                <i class="fa-solid fa-circle-notch fa-spin text-2xl mb-2 text-emerald-400"></i><br>
                                Fetching live servers from database...
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>

    </main>

    <!-- FOOTER -->
    <footer class="bg-[#0b0c0e] py-4 border-t border-[#1c222b] text-center text-xs text-gray-500">
        &copy; 2026 Alif Go VPN. Complete dynamic synchronization endpoint established.
    </footer>

    <!-- MAIN DASHBOARD JS LOGIC -->
    <script>
        // Simple mock authentication for testing
        if (!localStorage.getItem('alifAdminLoggedIn')) {
            // Show prompt
            const pass = prompt("Alif VPN Admin Password:", "admin123");
            if (pass === "admin123" || pass === "") {
                localStorage.setItem('alifAdminLoggedIn', 'true');
            } else {
                alert("Incorrect password. Access denied.");
                document.body.innerHTML = "<h2 style='color:red;padding:40px;text-align:center;'>Access Denied. Reload to retry.</h2>";
            }
        }

        document.getElementById('logoutBtn').addEventListener('click', () => {
            localStorage.removeItem('alifAdminLoggedIn');
            window.location.reload();
        });

        // Load VPN servers from our PHP API
        function loadServers() {
            const tableBody = document.getElementById('serverTableBody');
            
            fetch('get_vpn_servers.php')
                .then(response => response.json())
                .then(data => {
                    tableBody.innerHTML = '';
                    if (!data || data.length === 0) {
                        tableBody.innerHTML = `
                            <tr>
                                <td colspan="5" class="py-12 text-center text-gray-500">
                                    <i class="fa-solid fa-folder-open text-3xl mb-2"></i><br>
                                    No servers configured in database yet. Add one above!
                                </td>
                            </tr>
                        `;
                        return;
                    }

                    data.forEach(server => {
                        let badgeColor = "bg-emerald-500/10 text-emerald-400 border-emerald-500/20";
                        if (server.type === "Premium") badgeColor = "bg-blue-500/10 text-blue-400 border-blue-500/20";
                        if (server.type === "Gaming") badgeColor = "bg-amber-500/10 text-amber-400 border-amber-500/20";
                        if (server.type === "Streaming") badgeColor = "bg-purple-500/10 text-purple-400 border-purple-500/20";

                        const flagChar = getFlagEmoji(server.countryCode);

                        tableBody.innerHTML += `
                            <tr class="hover:bg-[#1a202a] transition duration-150">
                                <td class="py-3.5 pr-3">
                                    <div class="flex items-center space-x-2.5">
                                        <span class="text-xl">${flagChar}</span>
                                        <div>
                                            <div class="font-bold text-white text-sm">${server.countryName}</div>
                                            <div class="text-gray-400 text-xs">${server.city || 'Anywhere'}</div>
                                        </div>
                                    </div>
                                </td>
                                <td class="py-3.5 px-3 text-sm font-mono text-gray-300">
                                    ${server.ipAddress}
                                </td>
                                <td class="py-3.5 px-3">
                                    <span class="text-xs border px-2.5 py-0.5 rounded-full font-semibold ${badgeColor}">
                                        ${server.type}
                                    </span>
                                </td>
                                <td class="py-3.5 px-3 text-xs text-gray-400">
                                    ${server.protocol}
                                </td>
                                <td class="py-3.5 pl-3 text-right">
                                    <button onclick="deleteServer(${server.id})" class="text-rose-500 hover:text-rose-400 p-2 hover:bg-rose-500/10 rounded-lg transition">
                                        <i class="fa-solid fa-trash-can"></i>
                                    </button>
                                </td>
                            </tr>
                        `;
                    });
                })
                .catch(error => {
                    console.error('Error fetching servers:', error);
                    // Use static fallback simulation if PHP is not fully set up locally yet
                    tableBody.innerHTML = `
                        <tr>
                            <td colspan="5" class="py-6 text-center text-amber-400 text-xs">
                                <i class="fa-solid fa-triangle-exclamation mr-1.5"></i> 
                                Could not connect to local PHP Database. Showing offline simulation.
                            </td>
                        </tr>
                    `;
                });
        }

        // Add server via POST
        document.getElementById('addServerForm').addEventListener('submit', (e) => {
            e.preventDefault();
            
            const payload = {
                countryName: document.getElementById('countryName').value,
                countryCode: document.getElementById('countryCode').value.toUpperCase(),
                city: document.getElementById('city').value || "Any",
                ipAddress: document.getElementById('ipAddress').value,
                type: document.getElementById('type').value,
                protocol: document.getElementById('protocol').value,
                latency: parseInt(document.getElementById('latency').value) || 35,
                loadPercent: parseInt(document.getElementById('loadPercent').value) || 12
            };

            fetch('add_server.php', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            })
            .then(res => res.json())
            .then(data => {
                if (data.status === 'success') {
                    alert('Successfully added and published live server!');
                    document.getElementById('addServerForm').reset();
                    loadServers();
                } else {
                    alert('Error adding server: ' + data.message);
                }
            })
            .catch(err => {
                alert('Add server failed. Database connects when uploaded online.');
            });
        });

        // Delete Server via API
        function deleteServer(id) {
            if (confirm("Are you sure you want to delete this server relay? This will immediately disconnect active Android users!")) {
                fetch('delete_server.php', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ id: id })
                })
                .then(res => res.json())
                .then(data => {
                    if (data.status === 'success') {
                        loadServers();
                    } else {
                        alert('Error: ' + data.message);
                    }
                })
                .catch(err => {
                    alert('Action failed. Set up MySQL database online to execute.');
                });
            }
        }

        // Simple helper to get flag emoji
        function getFlagEmoji(countryCode) {
            const codePoints = countryCode
                .toUpperCase()
                .split('')
                .map(char =>  127397 + char.charCodeAt(0));
            try {
                return String.fromCodePoint(...codePoints);
            } catch(e) {
                return "🌍";
            }
        }

        // Process single .ovpn file upload on web dashboard!
        document.getElementById('ovpnFile').addEventListener('change', function(e) {
            const file = e.target.files[0];
            if (!file) return;

            const reader = new FileReader();
            reader.onload = function(evt) {
                const text = evt.target.result;
                // Parse remote IP
                const remoteMatch = text.match(/remote\s+([0-9a-zA-Z\.\-]+)/i);
                if (remoteMatch) {
                    let rawIp = remoteMatch[1];
                    document.getElementById('ipAddress').value = rawIp;
                    
                    // Try to guess from file name
                    const name = file.name.split('.')[0];
                    const parts = name.split(/[-_]/);
                    if (parts.length > 0) {
                        const code = parts[0].toUpperCase();
                        if (code.length === 2) {
                            document.getElementById('countryCode').value = code;
                            const map = { "US": "United States", "SG": "Singapore", "BD": "Bangladesh", "JP": "Japan", "KR": "South Korea", "DE": "Germany" };
                            document.getElementById('countryName').value = map[code] || parts[0];
                        } else {
                            document.getElementById('countryName').value = parts[0];
                            document.getElementById('countryCode').value = "SG";
                        }
                    }
                    alert('Successfully extracted IP address: ' + rawIp + '! Please verify details and click Publish.');
                } else {
                    alert('Could not auto-parse remote IP from config file. Enter manually.');
                }
            };
            reader.readAsText(file);
        });

        // Initialize table
        loadServers();
    </script>
</body>
</html>
