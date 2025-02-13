document.addEventListener('DOMContentLoaded', function() {
    const photosInput = document.getElementById('photosInput');
    const fileList = document.getElementById('fileList');
    const errorMessage = document.getElementById('errorMessage');
    const loadingOverlay = document.getElementById('loadingOverlay');
    const photoForm = document.getElementById('photoForm');
    let uploadedPhotos = []; // Array to store uploaded photos

    // Check if elements exist before using them
    if (!photosInput || !fileList || !errorMessage || !loadingOverlay || !photoForm) {
        console.error('Required elements not found');
        return;
    }

    // Define removeFile in the proper scope and make it globally accessible
    window.removeFile = function(index) {
        uploadedPhotos.splice(index, 1); // Remove the file at the given index
        renderFileList(); // Re-render the file list
    };

    function renderFileList() {
        fileList.innerHTML = ""; // Clear the file list container

        uploadedPhotos.forEach((file, index) => {
            const fileItem = document.createElement('div');
            fileItem.classList.add('flex', 'items-center', 'justify-between', 'py-1');
            fileItem.innerHTML = `
                ${file.name}
                <button type="button" onclick="removeFile(${index})" class="text-red-500 hover:text-red-700">Remove</button>
            `;
            fileList.appendChild(fileItem);
        });
    }

    photosInput.addEventListener('change', function () {
        errorMessage.classList.add('hidden');

        // Append new files to the existing uploadedPhotos array
        const newFiles = Array.from(photosInput.files);
        const totalFiles = uploadedPhotos.length + newFiles.length;

        if (totalFiles > 8) {
            errorMessage.textContent = "You can only upload up to 8 photos.";
            errorMessage.classList.remove('hidden');
            return;
        }

        // Add new files to the uploadedPhotos array
        uploadedPhotos = [...uploadedPhotos, ...newFiles];

        // Render the uploaded photos in the fileList container
        renderFileList();
    });

    photoForm.addEventListener('submit', async function (event) {
        event.preventDefault();
        if (uploadedPhotos.length !== 8) {
            errorMessage.textContent = "Please upload exactly 8 photos.";
            errorMessage.classList.remove('hidden');
            return;
        }
        const formData = new FormData(this);
        uploadedPhotos.forEach((file) => {
            formData.append('photos', file);
        });

        // Show the loading overlay
        loadingOverlay.classList.add('active');

        try {
            const response = await fetch('/api/submit', {
                method: 'POST',
                body: formData,
            });
            if (response.ok) {
                const data = await response.json();
                window.location.href = data.checkoutUrl;
            } else {
                alert('Error submitting photos. Please try again.');
            }
        } catch (error) {
            console.error('Error:', error);
            alert('An unexpected error occurred. Please try again.');
        } finally {
            // Hide the loading overlay
            loadingOverlay.classList.remove('active');
        }
    });
});


function toggleFAQ(id) {
    const content = document.getElementById(`faq-content-${id}`);
    const arrow = document.getElementById(`arrow-${id}`);

    // Toggle the content
    content.classList.toggle('hidden');

    // Rotate the arrow
    arrow.classList.toggle('rotate-180');

    // Close other FAQs
    for(let i = 1; i <= 6; i++) {
        if(i !== id) {
            const otherContent = document.getElementById(`faq-content-${i}`);
            const otherArrow = document.getElementById(`arrow-${i}`);
            if(!otherContent.classList.contains('hidden')) {
                otherContent.classList.add('hidden');
                otherArrow.classList.remove('rotate-180');
            }
        }
    }
}