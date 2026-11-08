using System;
using System.IO;

namespace IdleLoadshedding.Session
{
    /// <summary>Injected wall clock, so tests can roll time forward and back.</summary>
    public interface IClock
    {
        long UtcNowMs { get; }
    }

    public sealed class SystemClock : IClock
    {
        public long UtcNowMs => DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
    }

    /// <summary>The few file operations the save store needs, so tests can inject crashes between them.</summary>
    public interface ISaveFileSystem
    {
        bool Exists(string path);
        byte[] ReadAllBytes(string path);
        /// <summary>Creates or truncates the file, writes all bytes and flushes them to the device.</summary>
        void WriteAllBytesDurable(string path, byte[] data);
        /// <summary>Renames a file. The destination must not exist.</summary>
        void Move(string from, string to);
        void Copy(string from, string to);
        void Delete(string path);
        void CreateDirectory(string path);
    }

    public sealed class LocalFileSystem : ISaveFileSystem
    {
        public bool Exists(string path) => File.Exists(path);

        public byte[] ReadAllBytes(string path) => File.ReadAllBytes(path);

        public void WriteAllBytesDurable(string path, byte[] data)
        {
            using (var stream = new FileStream(path, FileMode.Create, FileAccess.Write, FileShare.None, 4096, FileOptions.WriteThrough))
            {
                stream.Write(data, 0, data.Length);
                stream.Flush(true);
            }
        }

        public void Move(string from, string to) => File.Move(from, to);

        public void Copy(string from, string to) => File.Copy(from, to, true);

        public void Delete(string path)
        {
            if (File.Exists(path)) File.Delete(path);
        }

        public void CreateDirectory(string path) => Directory.CreateDirectory(path);
    }
}
