
document.addEventListener('DOMContentLoaded', function () {
    const fechaInputs = document.querySelectorAll('input[type="date"]');
    const today = new Date().toISOString().split('T')[0];
    fechaInputs.forEach(input => input.setAttribute('min', today));

    // 👉 Si existe el canvas, dibujamos la gráfica
    if (window.etiquetas && window.cantidades && etiquetas.length > 0) {
        const ctx = document.getElementById('lineChart').getContext('2d');
        const lineChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: etiquetas,
                datasets: [{
                    label: 'Cantidad Entregada por Pedido',
                    data: cantidades,
                    borderColor: 'rgba(255, 99, 132, 1)',
                    backgroundColor: 'rgba(255, 99, 132, 0.2)',
                    fill: true,
                    tension: 0.4,
                    pointRadius: 5,
                    pointHoverRadius: 7
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: { display: true },
                    title: { display: true, text: 'Progreso de Pedidos Completados' }
                },
                scales: {
                    x: {
                        title: { display: true, text: 'Pedidos (ID)' }
                    },
                    y: {
                        beginAtZero: true,
                        title: { display: true, text: 'Cantidad Entregada' }
                    }
                }
            }
        });
    }
});