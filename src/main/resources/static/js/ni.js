// Modal functionality — lo puedes adaptar según tu HTML final
const open = document.getElementById('open');
const modal_container = document.getElementById('modal_container');
const close = document.getElementById('close');

if (open && close && modal_container) {
    open.addEventListener('click', () => {
        modal_container.classList.add('show');
    });

    close.addEventListener('click', () => {
        modal_container.classList.remove('show');
    });
}

// Validación rápida (puedes agregar más lógica si deseas)
const form = document.getElementById('formReg');
if (form) {
    form.addEventListener('submit', function (e) {
        // Validaciones personalizadas aquí
        console.log("Formulario enviado");
    });
}
